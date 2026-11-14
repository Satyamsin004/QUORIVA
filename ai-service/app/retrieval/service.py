"""
CS Scholar AI - Hybrid Retrieval Service Orchestrator
Coordinates query processing, dense pgvector retrieval, PostgreSQL full-text search,
Reciprocal Rank Fusion, and cross-encoder reranking.
"""
import logging
from typing import Optional
from uuid import UUID
from sqlalchemy.orm import Session

from app.retrieval.config import retrieval_config
from app.retrieval.query_processor import QueryProcessor
from app.retrieval.dense_retriever import DenseRetriever
from app.retrieval.keyword_retriever import KeywordRetriever
from app.retrieval.rrf import ReciprocalRankFusion
from app.retrieval.reranker import CrossEncoderReranker
from app.retrieval.schemas import (
    InternalSearchRequest,
    InternalSearchResponse,
    SearchResultItem
)

logger = logging.getLogger(__name__)


class RetrievalService:
    """
    Main orchestration service for the hybrid retrieval engine.
    Ensures SQL-level workspace isolation and multi-stage candidate reranking.
    """

    def __init__(
        self,
        query_processor: Optional[QueryProcessor] = None,
        dense_retriever: Optional[DenseRetriever] = None,
        keyword_retriever: Optional[KeywordRetriever] = None,
        rrf: Optional[ReciprocalRankFusion] = None,
        reranker: Optional[CrossEncoderReranker] = None
    ):
        self.query_processor = query_processor or QueryProcessor()
        self.dense_retriever = dense_retriever or DenseRetriever()
        self.keyword_retriever = keyword_retriever or KeywordRetriever()
        self.rrf = rrf or ReciprocalRankFusion()
        self.reranker = reranker or CrossEncoderReranker.get_instance()

    def search(self, db: Session, request: InternalSearchRequest) -> InternalSearchResponse:
        """
        Execute full hybrid retrieval pipeline for an authorized workspace.

        Args:
            db: Active database session.
            request: Validated internal search request from Spring Boot.

        Returns:
            InternalSearchResponse containing ranked, deduplicated academic chunks.
        """
        raw_query = request.query
        workspace_id = request.workspace_id
        top_k = request.top_k or retrieval_config.reranker_top_k
        dense_k = request.dense_candidate_count or retrieval_config.dense_candidate_count
        keyword_k = request.keyword_candidate_count or retrieval_config.keyword_candidate_count
        hybrid_k = request.hybrid_candidate_count or retrieval_config.hybrid_candidate_count
        rrf_k = request.rrf_k or retrieval_config.rrf_k

        logger.info(
            "Starting hybrid retrieval: workspace=%s, user=%s, top_k=%d, dense_k=%d, keyword_k=%d, hybrid_k=%d, rrf_k=%d",
            workspace_id, request.user_id, top_k, dense_k, keyword_k, hybrid_k, rrf_k
        )

        # 1. Clean and validate query, format BGE instructed query
        clean_query, instructed_query = self.query_processor.process(raw_query)

        # 2. Dense semantic retrieval
        query_embedding = self.dense_retriever.generate_query_embedding(instructed_query)
        dense_candidates = self.dense_retriever.search(
            db=db,
            workspace_id=workspace_id,
            query_vector=query_embedding,
            candidate_count=dense_k
        )

        # 3. PostgreSQL full-text keyword retrieval
        keyword_candidates = self.keyword_retriever.search(
            db=db,
            workspace_id=workspace_id,
            query_text=clean_query,
            candidate_count=keyword_k
        )

        # 4. Reciprocal Rank Fusion (RRF)
        hybrid_candidates = self.rrf.fuse(
            dense_candidates=dense_candidates,
            keyword_candidates=keyword_candidates,
            k=rrf_k,
            limit=hybrid_k
        )

        # 5. Cross-Encoder Reranking
        reranked_results = self.reranker.rerank(
            query=clean_query,
            candidates=hybrid_candidates,
            top_k=top_k
        )

        # 6. Map to response items
        items = [
            SearchResultItem(
                chunk_id=res.chunk_id,
                document_id=res.document_id,
                document_name=res.document_name,
                workspace_id=res.workspace_id,
                chunk_content=res.chunk_content,
                page_number=res.page_number,
                slide_number=res.slide_number,
                section_heading=res.section_heading,
                chunk_index=res.chunk_index,
                source_type=res.source_type,
                final_rank=res.final_rank,
                reranker_score=res.reranker_score,
                rrf_score=res.rrf_score,
                dense_rank=res.dense_rank,
                keyword_rank=res.keyword_rank,
                dense_similarity=res.dense_similarity,
                keyword_score=res.keyword_score,
                segment_index=res.segment_index
            )
            for res in reranked_results
        ]

        logger.info(
            "Hybrid retrieval completed: %d final results returned for workspace %s",
            len(items), workspace_id
        )

        return InternalSearchResponse(
            query=clean_query,
            workspace_id=workspace_id,
            total_candidates=len(hybrid_candidates),
            results=items
        )

