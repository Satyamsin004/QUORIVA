"""
CS Scholar AI - Celery Embedding Tasks
Asynchronous embedding generation tasks powered by Celery & Redis.
"""
import logging
import uuid
from typing import Optional, Dict, Any
from celery.exceptions import MaxRetriesExceededError

from app.workers.celery_app import celery_app
from app.embeddings.processor import EmbeddingProcessor
from app.embeddings.repository import EmbeddingRepository
from app.embeddings.config import embedding_config

logger = logging.getLogger(__name__)


@celery_app.task(bind=True, max_retries=3)
def generate_document_embeddings(
    self,
    document_id: str,
    processing_version: int = 1,
    job_id: Optional[str] = None,
    force_reembed: bool = False
) -> Dict[str, Any]:
    """
    Celery task to asynchronously generate and persist pgvector embeddings for a document's chunks.
    Retries up to 3 times with exponential backoff on failure.
    """
    logger.info(
        "Executing generate_document_embeddings: task_id=%s, document_id=%s, attempt=%d",
        self.request.id, document_id, self.request.retries + 1
    )

    doc_uuid = uuid.UUID(document_id)
    job_uuid = uuid.UUID(job_id) if job_id else None

    # Ensure EmbeddingJob is created or retrieved
    if not job_uuid:
        job = EmbeddingRepository.get_or_create_job(
            document_id=doc_uuid,
            model_name=embedding_config.model_name,
            model_version=embedding_config.model_version,
            processing_version=processing_version,
            task_id=self.request.id
        )
        job_uuid = job.id

    processor = EmbeddingProcessor()

    try:
        result = processor.process_document_embeddings(
            document_id=doc_uuid,
            processing_version=processing_version,
            job_id=job_uuid,
            force_reembed=force_reembed
        )
        return result

    except Exception as exc:
        logger.warning(
            "generate_document_embeddings failed on attempt %d for document %s: %s",
            self.request.retries + 1, document_id, exc
        )
        try:
            # Exponential backoff: attempt 0 -> 10s, attempt 1 -> 30s, attempt 2 -> 90s
            countdown = 10 * (3 ** self.request.retries)
            if job_uuid:
                EmbeddingRepository.update_job_status(job_uuid, "RETRYING", error_message=str(exc))
            raise self.retry(exc=exc, countdown=countdown)
        except MaxRetriesExceededError:
            logger.error("generate_document_embeddings max retries exceeded for document %s", document_id)
            if job_uuid:
                EmbeddingRepository.update_job_status(job_uuid, "FAILED", error_message=str(exc))
            EmbeddingRepository.update_document_embedding_status(doc_uuid, "FAILED")
            raise exc

