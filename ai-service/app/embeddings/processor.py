"""
CS Scholar AI - Document Embedding Processor
Orchestrates chunk segmentation, model inference, validation, and pgvector persistence.
"""
import uuid
import hashlib
import logging
from typing import Optional, Dict, Any, List, Tuple
import numpy as np

from app.embeddings.config import embedding_config
from app.embeddings.tokenizer import EmbeddingTokenizer
from app.embeddings.generator import EmbeddingGenerator
from app.embeddings.repository import EmbeddingRepository
from app.database.connection import get_db_session
from app.database.models import ChunkEmbedding, DocumentChunk, Document
from app.embeddings.exceptions import EmbeddingError

logger = logging.getLogger(__name__)


class EmbeddingProcessor:
    """End-to-end processor for transforming document chunks into pgvector embeddings."""

    def __init__(
        self,
        tokenizer: Optional[EmbeddingTokenizer] = None,
        generator: Optional[EmbeddingGenerator] = None,
        repository: Optional[EmbeddingRepository] = None
    ):
        self.tokenizer = tokenizer or EmbeddingTokenizer.get_instance()
        self.generator = generator or EmbeddingGenerator()
        self.repository = repository or EmbeddingRepository()

    @staticmethod
    def _compute_hash(text: str) -> str:
        """Compute SHA-256 hash for segment text."""
        return hashlib.sha256(text.encode("utf-8")).hexdigest()

    def process_document_embeddings(
        self,
        document_id: uuid.UUID,
        processing_version: int = 1,
        job_id: Optional[uuid.UUID] = None,
        force_reembed: bool = False
    ) -> Dict[str, Any]:
        """
        Generate and persist embeddings for all chunks of a document.
        
        Args:
            document_id: UUID of the document.
            processing_version: Ingestion processing version (default: 1).
            job_id: Optional UUID of the EmbeddingJob.
            force_reembed: If True, recompute all embeddings regardless of content hash.
            
        Returns:
            Dictionary with processing statistics.
        """
        model_name = embedding_config.model_name
        model_version = embedding_config.model_version

        logger.info(
            "Starting embedding generation: document_id=%s, model=%s, version=%d, force=%s",
            document_id, model_name, processing_version, force_reembed
        )

        with get_db_session() as db:
            doc = db.query(Document).filter(Document.id == document_id).first()
            if not doc or doc.deleted_at is not None:
                logger.info("Document %s is deleted or missing. Skipping embedding generation.", document_id)
                return {"document_id": str(document_id), "status": "SKIPPED_DELETED"}

        # 1. Update status to PROCESSING
        self.repository.update_document_embedding_status(document_id, "PROCESSING")
        if job_id:
            self.repository.update_job_status(job_id, "PROCESSING", increment_attempts=True)

        try:
            # 2. Load document chunks
            chunks: List[DocumentChunk] = self.repository.get_document_chunks(
                document_id, processing_version
            )

            if not chunks:
                logger.warning("No chunks found for document %s (version %d).", document_id, processing_version)
                self.repository.update_document_embedding_status(document_id, "COMPLETED")
                if job_id:
                    self.repository.update_job_status(job_id, "COMPLETED", chunks_processed=0, embeddings_generated=0)
                return {
                    "document_id": str(document_id),
                    "chunks_processed": 0,
                    "embeddings_generated": 0,
                    "status": "COMPLETED"
                }

            # 3. Tokenize & segment oversized chunks
            # A chunk may yield 1 or more segments depending on token length
            segments_to_process: List[Dict[str, Any]] = []
            chunk_ids: List[uuid.UUID] = []

            for chunk in chunks:
                chunk_ids.append(chunk.id)
                segmented = self.tokenizer.segment_text(chunk.content)

                for seg_idx, (seg_text, seg_tokens) in enumerate(segmented):
                    seg_hash = self._compute_hash(seg_text)
                    segments_to_process.append({
                        "chunk_id": chunk.id,
                        "document_id": document_id,
                        "segment_index": seg_idx,
                        "text": seg_text,
                        "token_count": seg_tokens,
                        "content_hash": seg_hash
                    })

            # 4. Check existing embeddings to skip unchanged segments
            existing_hashes: Dict[Tuple[uuid.UUID, int], str] = {}
            if not force_reembed:
                existing_hashes = self.repository.get_existing_embeddings_hashes(
                    chunk_ids, model_name, model_version
                )

            segments_needing_inference: List[Dict[str, Any]] = []
            skipped_count = 0

            for seg in segments_to_process:
                key = (seg["chunk_id"], seg["segment_index"])
                if key in existing_hashes and existing_hashes[key] == seg["content_hash"]:
                    skipped_count += 1
                else:
                    segments_needing_inference.append(seg)

            logger.info(
                "Document %s: %d total segments, %d need embedding, %d skipped (unchanged).",
                document_id, len(segments_to_process), len(segments_needing_inference), skipped_count
            )

            # 5. Generate embeddings in batches of 64 with early deletion check
            persisted_count = 0
            if segments_needing_inference:
                # Delete previous embeddings for modified chunks once upfront
                modified_chunk_ids = list({s["chunk_id"] for s in segments_needing_inference})
                self.repository.delete_embeddings_for_chunks(modified_chunk_ids, model_name, model_version)

                # Respect the configured inference batch size.  A fixed 64-item batch
                # can exhaust CPU/GPU memory and make every later document appear stuck.
                BATCH_CHUNK_SIZE = max(1, embedding_config.batch_size)
                for i in range(0, len(segments_needing_inference), BATCH_CHUNK_SIZE):
                    # Check if document was deleted mid-generation
                    with get_db_session() as db:
                        doc_check = db.query(Document).filter(Document.id == document_id).first()
                        if not doc_check or doc_check.deleted_at is not None:
                            logger.info("Document %s was deleted during embedding generation. Aborting early.", document_id)
                            return {"document_id": str(document_id), "status": "ABORTED_DELETED"}

                    batch_segs = segments_needing_inference[i : i + BATCH_CHUNK_SIZE]
                    batch_texts = [s["text"] for s in batch_segs]
                    batch_vectors = self.generator.generate_embeddings(batch_texts)

                    batch_records: List[ChunkEmbedding] = []
                    for seg, vec in zip(batch_segs, batch_vectors):
                        batch_records.append(ChunkEmbedding(
                            chunk_id=seg["chunk_id"],
                            document_id=seg["document_id"],
                            segment_index=seg["segment_index"],
                            embedding=vec.tolist(),
                            model_name=model_name,
                            model_version=model_version,
                            content_hash=seg["content_hash"],
                            token_count=seg["token_count"]
                        ))

                    # Verify that chunk_ids still exist in DB to prevent foreign key violations if chunks were regenerated
                    with get_db_session() as db:
                        existing_chunk_ids = set(
                            row[0] for row in db.query(DocumentChunk.id).filter(
                                DocumentChunk.id.in_([r.chunk_id for r in batch_records])
                            ).all()
                        )
                    valid_records = [r for r in batch_records if r.chunk_id in existing_chunk_ids]
                    if valid_records:
                        saved = self.repository.save_embeddings(valid_records)
                        persisted_count += saved
                        logger.info("Progress doc %s: saved %d/%d embeddings...", document_id, persisted_count, len(segments_needing_inference))

                        # Fast readiness: Mark document ready for AI search as soon as initial batch is persisted
                        # Dense retrieval and keyword search are immediately functional while background generation completes
                        if persisted_count >= min(32, len(segments_needing_inference)):
                            self.repository.update_document_embedding_status(document_id, "COMPLETED")
                    else:
                        logger.warning("Doc %s: chunks were replaced/deleted during embedding generation batch. Skipping batch.", document_id)

            # 8. Mark COMPLETED
            total_embeddings = skipped_count + persisted_count
            self.repository.update_document_embedding_status(document_id, "COMPLETED")
            if job_id:
                self.repository.update_job_status(
                    job_id,
                    status="COMPLETED",
                    chunks_processed=len(chunks),
                    embeddings_generated=total_embeddings
                )

            logger.info(
                "Embedding processing completed for document %s: chunks=%d, new_embeddings=%d, total=%d",
                document_id, len(chunks), persisted_count, total_embeddings
            )

            return {
                "document_id": str(document_id),
                "chunks_processed": len(chunks),
                "embeddings_generated": persisted_count,
                "total_embeddings": total_embeddings,
                "skipped_embeddings": skipped_count,
                "model_name": model_name,
                "model_version": model_version,
                "status": "COMPLETED"
            }

        except Exception as e:
            logger.error("Embedding processing failed for document %s: %s", document_id, e, exc_info=True)
            self.repository.update_document_embedding_status(document_id, "FAILED")
            if job_id:
                self.repository.update_job_status(job_id, "FAILED", error_message=str(e))
            raise

