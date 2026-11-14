"""
CS Scholar AI - Reciprocal Rank Fusion (RRF)
Combines dense semantic and PostgreSQL full-text keyword candidate lists into a unified hybrid ranking.
"""
import logging
from dataclasses import dataclass
from typing import List, Optional, Dict
from uuid import UUID

from app.retrieval.config import retrieval_config
from app.retrieval.dense_retriever import DenseCandidate
from app.retrieval.keyword_retriever import KeywordCandidate

logger = logging.getLogger(__name__)


@dataclass
class HybridCandidate:
    """Unified hybrid retrieval candidate after Reciprocal Rank Fusion."""

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
    rrf_score: float
    dense_rank: Optional[int]
    keyword_rank: Optional[int]
    dense_similarity: Optional[float]
    keyword_score: Optional[float]
    segment_index: Optional[int]


class ReciprocalRankFusion:
    """
    Computes RRF score across dense and keyword retrieval rankings:
      RRF_score(d) = Σ 1 / (k + rank_i(d))
    """

    def __init__(self, default_k: Optional[int] = None, default_limit: Optional[int] = None):
        self.default_k = default_k or retrieval_config.rrf_k
        self.default_limit = default_limit or retrieval_config.hybrid_candidate_count

    def fuse(
        self,
        dense_candidates: List[DenseCandidate],
        keyword_candidates: List[KeywordCandidate],
        k: Optional[int] = None,
        limit: Optional[int] = None
    ) -> List[HybridCandidate]:
        """
        Merge and deduplicate candidate lists via Reciprocal Rank Fusion.

        Args:
            dense_candidates: Candidates from DenseRetriever (sorted by dense_rank 1..N).
            keyword_candidates: Candidates from KeywordRetriever (sorted by keyword_rank 1..N).
            k: RRF constant parameter (default 60).
            limit: Maximum hybrid candidates to return (default 40).

        Returns:
            List of HybridCandidate sorted by descending rrf_score.
        """
        rrf_k = k if k is not None else self.default_k
        max_limit = limit if limit is not None else self.default_limit

        # Index candidates by chunk_id
        candidates_map: Dict[UUID, Dict] = {}

        # 1. Process dense candidates
        for cand in dense_candidates:
            cid = cand.chunk_id
            score = 1.0 / (rrf_k + cand.dense_rank)
            candidates_map[cid] = {
                "chunk_id": cid,
                "document_id": cand.document_id,
                "document_name": cand.document_name,
                "workspace_id": cand.workspace_id,
                "chunk_content": cand.chunk_content,
                "page_number": cand.page_number,
                "slide_number": cand.slide_number,
                "section_heading": cand.section_heading,
                "chunk_index": cand.chunk_index,
                "source_type": cand.source_type,
                "rrf_score": score,
                "dense_rank": cand.dense_rank,
                "keyword_rank": None,
                "dense_similarity": cand.dense_similarity,
                "keyword_score": None,
                "segment_index": cand.segment_index,
            }

        # 2. Process keyword candidates
        for cand in keyword_candidates:
            cid = cand.chunk_id
            score = 1.0 / (rrf_k + cand.keyword_rank)
            if cid in candidates_map:
                # Chunk was retrieved by both dense and keyword
                candidates_map[cid]["rrf_score"] += score
                candidates_map[cid]["keyword_rank"] = cand.keyword_rank
                candidates_map[cid]["keyword_score"] = cand.keyword_score
            else:
                # Chunk was retrieved by keyword search only
                candidates_map[cid] = {
                    "chunk_id": cid,
                    "document_id": cand.document_id,
                    "document_name": cand.document_name,
                    "workspace_id": cand.workspace_id,
                    "chunk_content": cand.chunk_content,
                    "page_number": cand.page_number,
                    "slide_number": cand.slide_number,
                    "section_heading": cand.section_heading,
                    "chunk_index": cand.chunk_index,
                    "source_type": cand.source_type,
                    "rrf_score": score,
                    "dense_rank": None,
                    "keyword_rank": cand.keyword_rank,
                    "dense_similarity": None,
                    "keyword_score": cand.keyword_score,
                    "segment_index": None,
                }

        # Deterministic sort: descending by rrf_score, tie-broken by dense_rank, keyword_rank, chunk_id
        sorted_candidates = sorted(
            candidates_map.values(),
            key=lambda c: (
                -c["rrf_score"],
                c["dense_rank"] if c["dense_rank"] is not None else 999999,
                c["keyword_rank"] if c["keyword_rank"] is not None else 999999,
                str(c["chunk_id"])
            )
        )

        # Slice to max_limit
        selected = sorted_candidates[:max_limit]

        results: List[HybridCandidate] = [
            HybridCandidate(
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
                rrf_score=float(item["rrf_score"]),
                dense_rank=item["dense_rank"],
                keyword_rank=item["keyword_rank"],
                dense_similarity=item["dense_similarity"],
                keyword_score=item["keyword_score"],
                segment_index=item["segment_index"]
            )
            for item in selected
        ]

        logger.info(
            "RRF fusion combined %d dense and %d keyword candidates into %d hybrid candidates (k=%d, limit=%d)",
            len(dense_candidates), len(keyword_candidates), len(results), rrf_k, max_limit
        )
        return results

