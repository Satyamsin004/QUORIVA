"""
CS Scholar AI - Internal Ingestion API Routes
Protected endpoints called by Spring Boot backend to trigger and monitor document ingestion.
"""
import uuid
import logging
from typing import Optional
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, status, Response
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session
import pymupdf

from app.database.connection import get_db
from app.database.models import Document, ProcessingJob
from app.database.storage import StorageClient
from app.api.routes.internal.security import verify_internal_service_key
from app.workers.tasks import process_document_task

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/internal/v1/ingestion",
    tags=["internal-ingestion"],
    dependencies=[Depends(verify_internal_service_key)]
)


class IngestionProcessRequest(BaseModel):
    document_id: uuid.UUID = Field(..., description="UUID of the uploaded document")
    workspace_id: uuid.UUID = Field(..., description="UUID of the workspace")
    storage_key: str = Field(..., description="S3 storage key of the document file")
    file_type: str = Field(..., description="Document type: PDF, DOCX, PPTX")
    mime_type: Optional[str] = Field(None, description="MIME type")
    file_size: Optional[int] = Field(None, description="File size in bytes")
    processing_version: int = Field(default=1, description="Processing schema version")


class IngestionJobResponse(BaseModel):
    job_id: uuid.UUID
    task_id: Optional[str] = None
    document_id: uuid.UUID
    status: str
    message: str


class JobStatusResponse(BaseModel):
    job_id: uuid.UUID
    document_id: uuid.UUID
    status: str
    attempts: int
    max_attempts: int
    error_message: Optional[str] = None
    task_id: Optional[str] = None
    processing_version: int
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    created_at: datetime
    updated_at: datetime


@router.post("/process", status_code=status.HTTP_202_ACCEPTED, response_model=IngestionJobResponse)
def trigger_document_ingestion(
    request: IngestionProcessRequest,
    db: Session = Depends(get_db)
):
    """
    Trigger asynchronous ingestion for a newly uploaded document.
    Enqueues background Celery task and initializes ProcessingJob.
    """
    doc: Optional[Document] = db.query(Document).filter(Document.id == request.document_id).first()
    if not doc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Document not found with id: {request.document_id}"
        )

    # Prevent duplicate active ingestion jobs for the same document
    existing_active_job = db.query(ProcessingJob).filter(
        ProcessingJob.document_id == request.document_id,
        ProcessingJob.status.in_(["QUEUED", "RUNNING", "PROCESSING"])
    ).first()
    if existing_active_job and doc.processing_status in ["PENDING_PROCESSING", "PROCESSING"]:
        logger.info("Ingestion job already active for document %s (job %s). Skipping duplicate dispatch.", request.document_id, existing_active_job.id)
        return IngestionJobResponse(
            job_id=existing_active_job.id,
            task_id=existing_active_job.task_id,
            document_id=request.document_id,
            status=existing_active_job.status,
            message="Ingestion job already active for this document"
        )

    # Create new ProcessingJob record
    job = ProcessingJob(
        document_id=request.document_id,
        status="QUEUED",
        attempts=0,
        max_attempts=3,
        processing_version=request.processing_version
    )
    db.add(job)

    # Transition document to PENDING_PROCESSING
    doc.processing_status = "PENDING_PROCESSING"
    doc.updated_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(job)

    # Dispatch Celery background task
    task_id_str = None
    try:
        task = process_document_task.delay(
            document_id=str(request.document_id),
            storage_key=request.storage_key,
            file_type=request.file_type,
            processing_version=request.processing_version,
            job_id=str(job.id)
        )
        task_id_str = task.id
        job.task_id = task_id_str
        db.commit()
    except Exception as e:
        logger.error("Failed to enqueue Celery task for document %s: %s", request.document_id, e)
        # Do not leave the material permanently "pending" when the broker is unavailable.
        # A later upload/retry must be able to create a clean job for this document.
        job.status = "FAILED"
        job.completed_at = datetime.now(timezone.utc)
        job.error_message = f"Failed to enqueue task: {e}"
        doc.processing_status = "FAILED"
        doc.updated_at = datetime.now(timezone.utc)
        db.commit()
        # The Spring outbox must see this as a dispatch failure and retry it;
        # returning 202 here would incorrectly mark the outbox as dispatched.
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Ingestion queue is temporarily unavailable"
        )

    return IngestionJobResponse(
        job_id=job.id,
        task_id=task_id_str,
        document_id=request.document_id,
        status=job.status,
        message="Document ingestion successfully queued for background processing."
    )


@router.get("/jobs/{job_id}", response_model=JobStatusResponse)
def get_job_status(
    job_id: uuid.UUID,
    db: Session = Depends(get_db)
):
    """Retrieve current processing status and details of an ingestion job."""
    job: Optional[ProcessingJob] = db.query(ProcessingJob).filter(ProcessingJob.id == job_id).first()
    if not job:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Processing job not found with id: {job_id}"
        )

    return JobStatusResponse(
        job_id=job.id,
        document_id=job.document_id,
        status=job.status,
        attempts=job.attempts,
        max_attempts=job.max_attempts,
        error_message=job.error_message,
        task_id=job.task_id,
        processing_version=job.processing_version,
        started_at=job.started_at,
        completed_at=job.completed_at,
        created_at=job.created_at,
        updated_at=job.updated_at
    )


@router.get("/page-preview")
def get_page_preview(
    storage_key: str,
    page_number: int = 1
):
    """Render a specific page of an uploaded PDF or slide document as a PNG image preview."""
    try:
        storage = StorageClient()
        with storage.download_to_tempfile(storage_key) as temp_path:
            doc = pymupdf.open(temp_path)
            try:
                total = doc.page_count
                if total == 0:
                    raise HTTPException(status_code=400, detail="Document has no pages")
                target_idx = max(0, min(page_number - 1, total - 1))
                page = doc.load_page(target_idx)
                # Render page to PNG at 150 DPI for sharp readability
                pix = page.get_pixmap(dpi=150)
                img_bytes = pix.tobytes("png")
                return Response(content=img_bytes, media_type="image/png")
            finally:
                doc.close()
    except HTTPException:
        raise
    except Exception as e:
        logger.error("Failed to render page preview for %s page %d: %s", storage_key, page_number, e, exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to generate page preview: {e}"
        )

