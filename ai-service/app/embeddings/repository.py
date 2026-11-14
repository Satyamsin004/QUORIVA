"""
CS Scholar AI - Embedding Database Repository
Database persistence and query operations for chunk embeddings and embedding lifecycle jobs.
"""
import uuid
import logging
from typing import List, Optional, Dict, Tuple
from datetime import datetime, timezone
from sqlalchemy.orm import Session
from sqlalchemy import and_
from sqlalchemy.dialects.postgresql import insert as pg_insert

from app.database.connection import get_db_session
from app.database.models import Document, DocumentChunk, ChunkEmbedding, EmbeddingJob
from app.embeddings.exceptions import EmbeddingPersistenceError

logger = logging.getLogger(__name__)


class EmbeddingRepository:
    """Repository handling pgvector persistence, chunk loading, and job state tracking."""

    @staticmethod
    def get_document_chunks(
        document_id: uuid.UUID,
        processing_version: int = 1,
        session: Optional[Session] = None
    ) -> List[DocumentChunk]:
        """Fetch all chunks for a document and processing version ordered by chunk_index."""
        def _query(db: Session):
            chunks = db.query(DocumentChunk).filter(
                and_(
                    DocumentChunk.document_id == document_id,
                    DocumentChunk.processing_version == processing_version
                )
            ).order_by(DocumentChunk.chunk_index.asc()).all()
            db.expunge_all()
            return chunks

        if session:
            return _query(session)
        with get_db_session() as db:
            return _query(db)

    @staticmethod
    def get_existing_embeddings_hashes(
        chunk_ids: List[uuid.UUID],
        model_name: str,
        model_version: str,
        session: Optional[Session] = None
    ) -> Dict[Tuple[uuid.UUID, int], str]:
        """
        Fetch existing content hashes mapped by (chunk_id, segment_index)
        to identify unchanged embeddings and avoid redundant recomputation.
        """
        if not chunk_ids:
            return {}

        def _query(db: Session):
            records = db.query(
                ChunkEmbedding.chunk_id,
                ChunkEmbedding.segment_index,
                ChunkEmbedding.content_hash
            ).filter(
                and_(
                    ChunkEmbedding.chunk_id.in_(chunk_ids),
                    ChunkEmbedding.model_name == model_name,
                    ChunkEmbedding.model_version == model_version
                )
            ).all()
            return {(r[0], r[1]): r[2] for r in records}

        if session:
            return _query(session)
        with get_db_session() as db:
            return _query(db)

    @staticmethod
    def save_embeddings(
        embeddings: List[ChunkEmbedding],
        session: Optional[Session] = None
    ) -> int:
        """
        Batch persist chunk embeddings.
        
        Args:
            embeddings: List of ChunkEmbedding model instances.
            session: Optional existing database session.
            
        Returns:
            Number of saved embeddings.
        """
        if not embeddings:
            return 0

        def _save(db: Session):
            try:
                data = [
                    {
                        "id": e.id or uuid.uuid4(),
                        "chunk_id": e.chunk_id,
                        "document_id": e.document_id,
                        "segment_index": e.segment_index,
                        "embedding": e.embedding,
                        "model_name": e.model_name,
                        "model_version": e.model_version,
                        "content_hash": e.content_hash,
                        "token_count": e.token_count,
                    }
                    for e in embeddings
                ]
                stmt = pg_insert(ChunkEmbedding).values(data)
                stmt = stmt.on_conflict_do_update(
                    constraint="uq_chunk_embedding",
                    set_={
                        "embedding": stmt.excluded.embedding,
                        "content_hash": stmt.excluded.content_hash,
                        "token_count": stmt.excluded.token_count,
                    }
                )
                db.execute(stmt)
                db.commit()
                return len(embeddings)
            except Exception as e:
                logger.error("Failed to persist chunk embeddings batch: %s", e, exc_info=True)
                raise EmbeddingPersistenceError(f"Database insertion failed: {e}")

        if session:
            return _save(session)
        with get_db_session() as db:
            return _save(db)

    @staticmethod
    def delete_embeddings_for_chunks(
        chunk_ids: List[uuid.UUID],
        model_name: str,
        model_version: str,
        session: Optional[Session] = None
    ) -> int:
        """Delete existing embeddings for specific chunks, model name, and version."""
        if not chunk_ids:
            return 0

        def _delete(db: Session):
            deleted = db.query(ChunkEmbedding).filter(
                and_(
                    ChunkEmbedding.chunk_id.in_(chunk_ids),
                    ChunkEmbedding.model_name == model_name,
                    ChunkEmbedding.model_version == model_version
                )
            ).delete(synchronize_session=False)
            return deleted

        if session:
            return _delete(session)
        with get_db_session() as db:
            return _delete(db)

    @staticmethod
    def update_document_embedding_status(
        document_id: uuid.UUID,
        status: str,
        session: Optional[Session] = None
    ) -> None:
        """Update embedding_status on the documents table."""
        def _update(db: Session):
            doc = db.query(Document).filter(Document.id == document_id).first()
            if doc:
                doc.embedding_status = status
                doc.updated_at = datetime.now(timezone.utc)

        if session:
            _update(session)
        else:
            with get_db_session() as db:
                _update(db)

    @staticmethod
    def get_or_create_job(
        document_id: uuid.UUID,
        model_name: str,
        model_version: str,
        processing_version: int = 1,
        chunks_total: int = 0,
        task_id: Optional[str] = None
    ) -> EmbeddingJob:
        """Create or update an EmbeddingJob record."""
        with get_db_session() as db:
            job = db.query(EmbeddingJob).filter(
                and_(
                    EmbeddingJob.document_id == document_id,
                    EmbeddingJob.model_name == model_name,
                    EmbeddingJob.model_version == model_version,
                    EmbeddingJob.processing_version == processing_version
                )
            ).order_by(EmbeddingJob.created_at.desc()).first()

            if not job:
                job = EmbeddingJob(
                    document_id=document_id,
                    job_type="EMBEDDING_GENERATION",
                    status="QUEUED",
                    model_name=model_name,
                    model_version=model_version,
                    processing_version=processing_version,
                    chunks_total=chunks_total,
                    task_id=task_id
                )
                db.add(job)
            else:
                job.status = "QUEUED"
                job.chunks_total = chunks_total
                if task_id:
                    job.task_id = task_id
                job.updated_at = datetime.now(timezone.utc)

            db.commit()
            db.refresh(job)
            return job

    @staticmethod
    def update_job_status(
        job_id: uuid.UUID,
        status: str,
        error_message: Optional[str] = None,
        chunks_processed: int = 0,
        embeddings_generated: int = 0,
        increment_attempts: bool = False
    ) -> None:
        """Update EmbeddingJob progress, state, and completion timestamps."""
        with get_db_session() as db:
            job = db.query(EmbeddingJob).filter(EmbeddingJob.id == job_id).first()
            if job:
                job.status = status
                if increment_attempts:
                    job.attempts += 1
                if status == "PROCESSING" and not job.started_at:
                    job.started_at = datetime.now(timezone.utc)
                if status in ("COMPLETED", "FAILED"):
                    job.completed_at = datetime.now(timezone.utc)
                if error_message:
                    job.error_message = error_message
                if chunks_processed > 0:
                    job.chunks_processed = chunks_processed
                if embeddings_generated > 0:
                    job.embeddings_generated = embeddings_generated
                job.updated_at = datetime.now(timezone.utc)
