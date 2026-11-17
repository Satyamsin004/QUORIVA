"""
CS Scholar AI - Document Ingestion Pipeline
Coordinates downloading from S3, format-specific parsing, structure-aware chunking,
and database persistence with transactional consistency and idempotency.
"""
import logging
import uuid
from datetime import datetime, timezone
from typing import Optional, Dict, Any, List

from sqlalchemy.orm import Session
from app.database.connection import get_db_session
from app.database.models import Document, DocumentChunk, ProcessingJob
from app.database.storage import storage_client
from app.ingestion.factory import ParserFactory
from app.ingestion.chunking.chunker import StructureAwareChunker
from app.ingestion.exceptions import IngestionError

logger = logging.getLogger(__name__)


class IngestionPipeline:
    """End-to-end document ingestion processing pipeline."""

    def __init__(self):
        self.parser_factory = ParserFactory()
        self.chunker = StructureAwareChunker()

    def process_document(
        self,
        document_id: uuid.UUID,
        storage_key: str,
        file_type: str,
        processing_version: int = 1,
        job_id: Optional[uuid.UUID] = None
    ) -> Dict[str, Any]:
        """
        Execute full ingestion pipeline for a document.
        
        Args:
            document_id: UUID of the document in PostgreSQL.
            storage_key: S3 object storage key.
            file_type: Document format ('PDF', 'DOCX', 'PPTX').
            processing_version: Schema / chunking version number (default 1).
            job_id: Optional UUID of the ProcessingJob record.
            
        Returns:
            Dictionary with ingestion statistics (chunk_count, total_tokens, total_pages, etc.)
        """
        logger.info(
            "Starting document ingestion pipeline: document_id=%s, storage_key=%s, file_type=%s, version=%d",
            document_id, storage_key, file_type, processing_version
        )

        with get_db_session() as db:
            doc: Optional[Document] = db.query(Document).filter(Document.id == document_id).first()
            if not doc or doc.deleted_at is not None:
                logger.info("Document %s is deleted or missing. Skipping ingestion pipeline.", document_id)
                return {"document_id": str(document_id), "status": "SKIPPED_DELETED"}

            doc_title = doc.title
            workspace_id = str(doc.workspace_id)

            job: Optional[ProcessingJob] = None
            if job_id:
                job = db.query(ProcessingJob).filter(ProcessingJob.id == job_id).first()
            if not job:
                job = db.query(ProcessingJob).filter(
                    ProcessingJob.document_id == document_id,
                    ProcessingJob.processing_version == processing_version
                ).order_by(ProcessingJob.created_at.desc()).first()

            if job:
                job.status = "PROCESSING"
                job.started_at = datetime.now(timezone.utc)
                job.updated_at = datetime.now(timezone.utc)
                job.attempts += 1
                db.flush()

            doc.processing_status = "PROCESSING"
            doc.updated_at = datetime.now(timezone.utc)
            db.flush()

        try:
            # 1. Download file to local temporary storage
            with storage_client.download_to_tempfile(storage_key, file_type) as temp_file_path:
                # 2. Resolve parser and parse document
                parser = self.parser_factory.get_parser(file_type, storage_key)
                parsed_doc = parser.parse(
                    temp_file_path,
                    metadata={
                        "document_id": str(document_id),
                        "storage_key": storage_key,
                        "file_type": file_type,
                        "title": doc_title
                    }
                )

                # 3. Structure-aware chunking
                chunk_models = self.chunker.chunk_document(
                    parsed_doc=parsed_doc,
                    document_metadata={
                        "document_id": str(document_id),
                        "workspace_id": workspace_id,
                        "processing_version": processing_version
                    }
                )

            # 4. Transactionally persist chunks and update statuses
            total_tokens = sum(c.token_count for c in chunk_models)
            page_count = parsed_doc.total_pages or parsed_doc.total_slides

            with get_db_session() as db:
                doc = db.query(Document).filter(Document.id == document_id).with_for_update().first()
                if not doc or doc.deleted_at is not None:
                    logger.info("Document %s was deleted or missing. Skipping chunk persistence.", document_id)
                    return {"document_id": str(document_id), "status": "SKIPPED_DELETED"}

                if page_count:
                    doc.page_count = page_count
                doc.processing_status = "PROCESSED"
                doc.updated_at = datetime.now(timezone.utc)

                # Remove any existing chunks for this version (idempotency)
                db.query(DocumentChunk).filter(
                    DocumentChunk.document_id == document_id,
                    DocumentChunk.processing_version == processing_version
                ).delete()

                # Bulk insert new chunks in safe batches of 250 to optimize memory & DB transactions
                db_chunks = [
                    DocumentChunk(
                        document_id=document_id,
                        chunk_index=chunk.chunk_index,
                        processing_version=chunk.processing_version,
                        page_number=chunk.page_number,
                        slide_number=chunk.slide_number,
                        section_title=chunk.section_title,
                        content=chunk.content,
                        token_count=chunk.token_count,
                        source_type=chunk.source_type,
                        extraction_method=chunk.extraction_method,
                        content_hash=chunk.content_hash,
                        extra_metadata=chunk.metadata
                    )
                    for chunk in chunk_models
                ]
                BATCH_INSERT_SIZE = 250
                for i in range(0, len(db_chunks), BATCH_INSERT_SIZE):
                    db.bulk_save_objects(db_chunks[i:i + BATCH_INSERT_SIZE])
                    db.flush()

                # Update job to COMPLETED
                if job_id or (job and job.id):
                    target_job_id = job_id or job.id
                    active_job = db.query(ProcessingJob).filter(ProcessingJob.id == target_job_id).first()
                    if active_job:
                        active_job.status = "COMPLETED"
                        active_job.completed_at = datetime.now(timezone.utc)
                        active_job.updated_at = datetime.now(timezone.utc)
                        active_job.error_message = None

            # 5. Automatically enqueue embedding generation for the processed chunks
            try:
                from app.workers.embedding_tasks import generate_document_embeddings
                generate_document_embeddings.delay(
                    document_id=str(document_id),
                    processing_version=processing_version
                )
                with get_db_session() as db:
                    doc = db.query(Document).filter(Document.id == document_id).first()
                    if doc and doc.embedding_status == "NOT_STARTED":
                        doc.embedding_status = "QUEUED"
                        doc.updated_at = datetime.now(timezone.utc)
                logger.info("Automatically enqueued embedding generation for document %s", document_id)
            except Exception as emb_err:
                # Surface a dispatch failure instead of showing an endless pending state.
                logger.warning("Could not auto-enqueue embedding task for document %s: %s", document_id, emb_err)
                with get_db_session() as db:
                    doc = db.query(Document).filter(Document.id == document_id).first()
                    if doc and doc.deleted_at is None:
                        doc.embedding_status = "FAILED"
                        doc.updated_at = datetime.now(timezone.utc)

            logger.info(
                "Document ingestion completed successfully: document_id=%s, chunks=%d, total_tokens=%d, pages=%s",
                document_id, len(chunk_models), total_tokens, page_count
            )

            return {
                "document_id": str(document_id),
                "chunks_created": len(chunk_models),
                "total_tokens": total_tokens,
                "page_count": page_count,
                "status": "PROCESSED"
            }

        except Exception as e:
            logger.error("Ingestion failed for document_id=%s: %s", document_id, e, exc_info=True)
            with get_db_session() as db:
                if job_id or (job and job.id):
                    target_job_id = job_id or job.id
                    active_job = db.query(ProcessingJob).filter(ProcessingJob.id == target_job_id).first()
                    if active_job:
                        active_job.status = "FAILED"
                        active_job.error_message = str(e)
                        active_job.updated_at = datetime.now(timezone.utc)

                doc = db.query(Document).filter(Document.id == document_id).first()
                if doc:
                    if doc.deleted_at is not None:
                        logger.info("Document %s was deleted by user; keeping DELETED status.", document_id)
                    elif doc.processing_status == "PROCESSED":
                        logger.info("Document %s is already PROCESSED by another task; keeping PROCESSED status.", document_id)
                    else:
                        doc.processing_status = "FAILED"
                        doc.updated_at = datetime.now(timezone.utc)

            raise
