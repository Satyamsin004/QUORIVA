"""
CS Scholar AI - Internal Embedding API Routes
Protected endpoints called by Spring Boot or internal maintenance to trigger and inspect embedding generation.
"""
import uuid
import logging
from typing import Optional
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app.database.connection import get_db
from app.database.models import Document, EmbeddingJob
from app.api.routes.internal.security import verify_internal_service_key
from app.workers.embedding_tasks import generate_document_embeddings
from app.embeddings.config import embedding_config

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/internal/v1/embeddings",
    tags=["internal-embeddings"],
    dependencies=[Depends(verify_internal_service_key)]
)


class EmbeddingProcessRequest(BaseModel):
    document_id: uuid.UUID = Field(..., description="UUID of the document to embed")
    processing_version: int = Field(default=1, description="Processing schema version")
    force_reembed: bool = Field(default=False, description="Whether to recompute existing embeddings")


class EmbeddingJobResponse(BaseModel):
    job_id: uuid.UUID
    task_id: Optional[str] = None
    document_id: uuid.UUID
    status: str
    message: str


class EmbeddingJobStatusResponse(BaseModel):
    job_id: uuid.UUID
    document_id: uuid.UUID
    status: str
    model_name: str
    model_version: str
    processing_version: int
    chunks_total: int
    chunks_processed: int
    embeddings_generated: int
    attempts: int
    max_attempts: int
    error_message: Optional[str] = None
    task_id: Optional[str] = None
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    created_at: datetime
    updated_at: datetime


@router.post("/process", status_code=status.HTTP_202_ACCEPTED, response_model=EmbeddingJobResponse)
def trigger_embedding_generation(
    request: EmbeddingProcessRequest,
    db: Session = Depends(get_db)
):
    """Trigger asynchronous embedding generation for a document's chunks."""
    doc: Optional[Document] = db.query(Document).filter(Document.id == request.document_id).first()
    if not doc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Document not found with id: {request.document_id}"
        )

    # Initialize EmbeddingJob record
    job = EmbeddingJob(
        document_id=request.document_id,
        job_type="EMBEDDING_GENERATION",
        status="QUEUED",
        model_name=embedding_config.model_name,
        model_version=embedding_config.model_version,
        processing_version=request.processing_version,
        attempts=0,
        max_attempts=3
    )
    db.add(job)

    # Transition document embedding status to QUEUED
    doc.embedding_status = "QUEUED"
    db.commit()
    db.refresh(job)

    # Dispatch Celery background task
    task_id_str = None
    try:
        task = generate_document_embeddings.delay(
            document_id=str(request.document_id),
            processing_version=request.processing_version,
            job_id=str(job.id),
            force_reembed=request.force_reembed
        )
        task_id_str = task.id
        job.task_id = task_id_str
        db.commit()
    except Exception as e:
        logger.error("Failed to enqueue embedding Celery task for document %s: %s", request.document_id, e)
        job.error_message = f"Failed to enqueue task: {e}"
        db.commit()

    return EmbeddingJobResponse(
        job_id=job.id,
        task_id=task_id_str,
        document_id=request.document_id,
        status=job.status,
        message="Embedding generation successfully queued for background processing."
    )


@router.get("/jobs/{job_id}", response_model=EmbeddingJobStatusResponse)
def get_embedding_job_status(
    job_id: uuid.UUID,
    db: Session = Depends(get_db)
):
    """Retrieve current status and statistics of an embedding generation job."""
    job: Optional[EmbeddingJob] = db.query(EmbeddingJob).filter(EmbeddingJob.id == job_id).first()
    if not job:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Embedding job not found with id: {job_id}"
        )

    return EmbeddingJobStatusResponse(
        job_id=job.id,
        document_id=job.document_id,
        status=job.status,
        model_name=job.model_name,
        model_version=job.model_version,
        processing_version=job.processing_version,
        chunks_total=job.chunks_total,
        chunks_processed=job.chunks_processed,
        embeddings_generated=job.embeddings_generated,
        attempts=job.attempts,
        max_attempts=job.max_attempts,
        error_message=job.error_message,
        task_id=job.task_id,
        started_at=job.started_at,
        completed_at=job.completed_at,
        created_at=job.created_at,
        updated_at=job.updated_at
    )


@router.post("/retry/{job_id}", status_code=status.HTTP_202_ACCEPTED, response_model=EmbeddingJobResponse)
def retry_failed_embedding_job(
    job_id: uuid.UUID,
    db: Session = Depends(get_db)
):
    """Retry a failed or stalled embedding job."""
    job: Optional[EmbeddingJob] = db.query(EmbeddingJob).filter(EmbeddingJob.id == job_id).first()
    if not job:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Embedding job not found with id: {job_id}"
        )

    job.status = "QUEUED"
    job.error_message = None
    db.commit()

    task = generate_document_embeddings.delay(
        document_id=str(job.document_id),
        processing_version=job.processing_version,
        job_id=str(job.id),
        force_reembed=False
    )
    job.task_id = task.id
    db.commit()

    return EmbeddingJobResponse(
        job_id=job.id,
        task_id=task.id,
        document_id=job.document_id,
        status=job.status,
        message="Embedding job successfully requeued."
    )

