"""
CS Scholar AI - Dense Semantic Retriever
Executes scoped pgvector similarity search, deduplicates multi-segment chunk matches,
and returns ranked candidates.
"""
import logging
from dataclasses import dataclass
from typing import List, Optional, Dict, Any
from uuid import UUID
import numpy as np
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.embeddings.generator import EmbeddingGenerator
from app.retrieval.config import retrieval_config
from app.retrieval.exceptions import DenseRetrievalError

logger = logging.getLogger(__name__)


@dataclass
class DenseCandidate:
    """Internal representation of a dense retrieval candidate."""

    chunk_id: UUID
    document_id: UUID
    document_name: str
    workspace_id: UUID
    chunk_content: str
    page_number: Optional[int]
    slide_number: Optional[int]
    section_heading: Optional[str]
    chunk_index: int
    source_type: str
    dense_rank: int
    cosine_distance: float
    dense_similarity: float
    segment_index: int


class DenseRetriever:
    """
    Performs scoped dense semantic search against pgvector chunk_embeddings.
    Deduplicates segments belonging to the same document chunk.
    """

    def __init__(
        self,
        embedding_generator: Optional[EmbeddingGenerator] = None,
        default_candidate_count: Optional[int] = None
    ):
        self.embedding_generator = embedding_generator or EmbeddingGenerator()
        self.default_candidate_count = default_candidate_count or retrieval_config.dense_candidate_count

    def generate_query_embedding(self, instructed_query: str) -> np.ndarray:
        """
        Embed the search query into a 768-dimensional normalized dense vector.
        """
        try:
            embeddings = self.embedding_generator.generate_embeddings([instructed_query])
            if not embeddings:
                raise DenseRetrievalError("Failed to generate query vector: empty output.")
            return embeddings[0]
        except Exception as e:
            logger.error("Query embedding generation error: %s", e, exc_info=True)
            raise DenseRetrievalError(f"Failed to generate query embedding: {e}")

    def search(
        self,
        db: Session,
        workspace_id: UUID,
        query_vector: np.ndarray,
        candidate_count: Optional[int] = None
    ) -> List[DenseCandidate]:
        """
        Execute scoped cosine distance search in PostgreSQL using pgvector <=> operator.

        Args:
            db: Active SQLAlchemy database session.
            workspace_id: Trusted workspace identifier for SQL-level isolation.
            query_vector: Normalized 768-dimensional float vector.
            candidate_count: Target number of unique chunk candidates.

        Returns:
            List of DenseCandidate sorted by cosine similarity descending (distance ascending).
        """
        limit = candidate_count or self.default_candidate_count
        # Fetch up to 2x limit to ensure enough distinct chunks after segment deduplication
        fetch_limit = max(limit * 2, 50)

        # Vector format for pgvector literal binding: '[0.1, 0.2, ...]'
        vector_str = "[" + ",".join(f"{float(x):.8f}" for x in query_vector) + "]"

        sql = text("""
            SELECT 
                ce.chunk_id,
                ce.document_id,
                ce.segment_index,
                (ce.embedding <=> :vector) AS distance,
                dc.chunk_index,
                dc.processing_version,
                dc.page_number,
                dc.slide_number,
                dc.section_title,
                dc.content,
                dc.token_count,
                dc.source_type,
                d.title AS document_name,
                d.workspace_id
            FROM chunk_embeddings ce
            JOIN documents d ON ce.document_id = d.id
            JOIN document_chunks dc ON ce.chunk_id = dc.id
            WHERE d.workspace_id = :workspace_id
              AND d.deleted_at IS NULL
            ORDER BY ce.embedding <=> :vector ASC
            LIMIT :fetch_limit
        """)

        try:
            # Set ef_search locally for high recall in HNSW index traversal
            try:
                db.execute(text("SET LOCAL hnsw.ef_search = 100"))
            except Exception:
                pass

            result = db.execute(sql, {
                "vector": vector_str,
                "workspace_id": workspace_id,
                "fetch_limit": fetch_limit
            })
            rows = result.fetchall()
        except Exception as e:
            logger.error("Dense retrieval database query failed: %s", e, exc_info=True)
            raise DenseRetrievalError(f"Dense vector retrieval query failed: {e}")

        # Deduplicate multi-segment chunks: retain segment with minimum distance (highest similarity)
        best_chunks: Dict[UUID, Dict[str, Any]] = {}
        for row in rows:
            chunk_id = row.chunk_id
            dist = float(row.distance)

            if chunk_id not in best_chunks or dist < best_chunks[chunk_id]["distance"]:
                best_chunks[chunk_id] = {
                    "chunk_id": chunk_id,
                    "document_id": row.document_id,
                    "document_name": row.document_name,
                    "workspace_id": row.workspace_id,
                    "chunk_content": row.content,
                    "page_number": row.page_number,
                    "slide_number": row.slide_number,
                    "section_heading": row.section_title,
                    "chunk_index": row.chunk_index,
                    "source_type": row.source_type,
                    "distance": dist,
                    "segment_index": row.segment_index,
                }

        # Sort deduplicated chunks by distance ascending (closest match first)
        sorted_chunks = sorted(best_chunks.values(), key=lambda c: (c["distance"], str(c["chunk_id"])))[:limit]

        candidates: List[DenseCandidate] = []
        for rank_idx, item in enumerate(sorted_chunks, start=1):
            distance = item["distance"]
            similarity = max(0.0, min(1.0, 1.0 - distance))
            candidates.append(
                DenseCandidate(
                    chunk_id=item["chunk_id"],
                    document_id=item["document_id"],
                    document_name=item["document_name"],
                    workspace_id=item["workspace_id"],
                    chunk_content=item["chunk_content"],
                    page_number=item["page_number"],
                    slide_number=item["slide_number"],
                    section_heading=item["section_heading"],
                    chunk_index=item["chunk_index"],
                    source_type=item["source_type"],
                    dense_rank=rank_idx,
                    cosine_distance=distance,
                    dense_similarity=similarity,
                    segment_index=item["segment_index"]
                )
            )

        logger.info(
            "Dense retrieval returned %d candidates for workspace %s (from %d scanned rows)",
            len(candidates), workspace_id, len(rows)
        )
        return candidates

