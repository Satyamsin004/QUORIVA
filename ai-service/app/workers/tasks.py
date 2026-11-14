"""
CS Scholar AI - Celery Ingestion Tasks
"""
import logging
import uuid
from typing import Optional, Dict, Any
from celery.exceptions import MaxRetriesExceededError

from app.workers.celery_app import celery_app
from app.ingestion.pipeline import IngestionPipeline

logger = logging.getLogger(__name__)


@celery_app.task(bind=True, max_retries=3)
def process_document_task(
    self,
    document_id: str,
    storage_key: str,
    file_type: str,
    processing_version: int = 1,
    job_id: Optional[str] = None
) -> Dict[str, Any]:
    """
    Celery task to asynchronously process and ingest a document.
    Retries up to 3 times with exponential backoff on failure.
    """
    logger.info("Executing process_document_task: task_id=%s, document_id=%s, attempt=%d",
                self.request.id, document_id, self.request.retries + 1)

    pipeline = IngestionPipeline()
    doc_uuid = uuid.UUID(document_id)
    job_uuid = uuid.UUID(job_id) if job_id else None

    try:
        result = pipeline.process_document(
            document_id=doc_uuid,
            storage_key=storage_key,
            file_type=file_type,
            processing_version=processing_version,
            job_id=job_uuid
        )
        return result
    except Exception as exc:
        logger.warning(
            "process_document_task failed on attempt %d for document %s: %s",
            self.request.retries + 1, document_id, exc
        )
        try:
            # Exponential backoff: attempt 0 -> 10s, attempt 1 -> 30s, attempt 2 -> 90s
            countdown = 10 * (3 ** self.request.retries)
            raise self.retry(exc=exc, countdown=countdown)
        except MaxRetriesExceededError:
            logger.error("process_document_task max retries exceeded for document %s", document_id)
            raise exc

