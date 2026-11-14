"""
CS Scholar AI - Retrieval Package
"""
from app.retrieval.config import retrieval_config, RetrievalConfig
from app.retrieval.exceptions import (
    RetrievalError,
    QueryValidationError,
    DenseRetrievalError,
    KeywordRetrievalError,
    RerankerError,
)
from app.retrieval.schemas import (
    InternalSearchRequest,
    InternalSearchResponse,
    SearchResultItem,
)
from app.retrieval.query_processor import QueryProcessor
from app.retrieval.dense_retriever import DenseRetriever, DenseCandidate
from app.retrieval.keyword_retriever import KeywordRetriever, KeywordCandidate
from app.retrieval.rrf import ReciprocalRankFusion, HybridCandidate
from app.retrieval.reranker import CrossEncoderReranker, RerankedResult
from app.retrieval.service import RetrievalService

__all__ = [
    "retrieval_config",
    "RetrievalConfig",
    "RetrievalError",
    "QueryValidationError",
    "DenseRetrievalError",
    "KeywordRetrievalError",
    "RerankerError",
    "InternalSearchRequest",
    "InternalSearchResponse",
    "SearchResultItem",
    "QueryProcessor",
    "DenseRetriever",
    "DenseCandidate",
    "KeywordRetriever",
    "KeywordCandidate",
    "ReciprocalRankFusion",
    "HybridCandidate",
    "CrossEncoderReranker",
    "RerankedResult",
    "RetrievalService",
]
