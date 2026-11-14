"""
CS Scholar AI - PostgreSQL Full-Text Keyword Retriever
Executes scoped keyword search using PostgreSQL tsvector, websearch_to_tsquery,
and ts_rank_cd cover density ranking.
"""
import logging
from dataclasses import dataclass
from typing import List, Optional
from uuid import UUID
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.retrieval.config import retrieval_config
from app.retrieval.exceptions import KeywordRetrievalError

logger = logging.getLogger(__name__)


@dataclass
class KeywordCandidate:
    """Internal representation of a PostgreSQL full-text keyword retrieval candidate."""

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
    keyword_rank: int
    keyword_score: float


class KeywordRetriever:
    """
    Performs scoped full-text keyword search in PostgreSQL using tsvector and ts_rank_cd.
    """

    def __init__(self, default_candidate_count: Optional[int] = None):
        self.default_candidate_count = default_candidate_count or retrieval_config.keyword_candidate_count

    def search(
        self,
        db: Session,
        workspace_id: UUID,
        query_text: str,
        candidate_count: Optional[int] = None
    ) -> List[KeywordCandidate]:
        """
        Execute scoped full-text search against document_chunks.content_tsv.

        Args:
            db: Active SQLAlchemy database session.
            workspace_id: Trusted workspace identifier for SQL-level isolation.
            query_text: User search query string.
            candidate_count: Maximum candidate chunks to retrieve.

        Returns:
            List of KeywordCandidate sorted by ts_rank_cd descending.
        """
        limit = candidate_count or self.default_candidate_count
        clean_query = query_text.strip()
        if not clean_query:
            return []

        # Check if the query parses to any lexemes using websearch_to_tsquery
        check_sql = text("SELECT websearch_to_tsquery('english', :query)::text AS tsquery_str")
        try:
            check_row = db.execute(check_sql, {"query": clean_query}).fetchone()
            tsquery_str = check_row.tsquery_str if check_row else ""
            if not tsquery_str or tsquery_str.strip() == "":
                logger.info("Keyword query '%s' yielded empty tsquery (e.g. stop words only). Returning empty list.", clean_query)
                return []
        except Exception as e:
            logger.warning("Error inspecting tsquery for '%s': %s", clean_query, e)

        sql = text("""
            SELECT 
                dc.id AS chunk_id,
                dc.document_id,
                dc.chunk_index,
                dc.processing_version,
                dc.page_number,
                dc.slide_number,
                dc.section_title,
                dc.content,
                dc.token_count,
                dc.source_type,
                d.title AS document_name,
                d.workspace_id,
                ts_rank_cd(dc.content_tsv, websearch_to_tsquery('english', :query)) AS rank_score
            FROM document_chunks dc
            JOIN documents d ON dc.document_id = d.id
            WHERE d.workspace_id = :workspace_id
              AND d.deleted_at IS NULL
              AND dc.content_tsv @@ websearch_to_tsquery('english', :query)
            ORDER BY rank_score DESC, dc.id ASC
            LIMIT :limit
        """)

        try:
            result = db.execute(sql, {
                "workspace_id": workspace_id,
                "query": clean_query,
                "limit": limit
            })
            rows = result.fetchall()
        except Exception as e:
            logger.error("PostgreSQL full-text keyword retrieval failed: %s", e, exc_info=True)
            raise KeywordRetrievalError(f"PostgreSQL full-text keyword retrieval query failed: {e}")

        candidates: List[KeywordCandidate] = []
        for rank_idx, row in enumerate(rows, start=1):
            candidates.append(
                KeywordCandidate(
                    chunk_id=row.chunk_id,
                    document_id=row.document_id,
                    document_name=row.document_name,
                    workspace_id=row.workspace_id,
                    chunk_content=row.content,
                    page_number=row.page_number,
                    slide_number=row.slide_number,
                    section_heading=row.section_title,
                    chunk_index=row.chunk_index,
                    source_type=row.source_type,
                    keyword_rank=rank_idx,
                    keyword_score=float(row.rank_score)
                )
            )

        logger.info(
            "Keyword retrieval returned %d candidates for workspace %s (query='%s')",
            len(candidates), workspace_id, clean_query
        )
        return candidates

