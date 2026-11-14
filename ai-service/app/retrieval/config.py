"""
CS Scholar AI - Retrieval Configuration
Runtime retrieval parameters for dense, keyword, RRF, and cross-encoder reranking.
"""
from dataclasses import dataclass
from typing import Optional
from app.core.config import settings


@dataclass
class RetrievalConfig:
    """Configuration parameters for the hybrid retrieval engine."""

    reranker_model_name: str = settings.reranker_model_name
    reranker_device: str = settings.reranker_device
    reranker_batch_size: int = settings.reranker_batch_size
    reranker_top_k: int = settings.reranker_top_k
    min_reranker_score: float = -2.5
    dense_candidate_count: int = settings.dense_candidate_count
    keyword_candidate_count: int = settings.keyword_candidate_count
    hybrid_candidate_count: int = settings.hybrid_candidate_count
    rrf_k: int = settings.rrf_k
    query_instruction: str = settings.query_instruction


retrieval_config = RetrievalConfig()

