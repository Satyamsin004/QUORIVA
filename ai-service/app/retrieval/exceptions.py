"""
CS Scholar AI - Retrieval Exceptions
Custom exception hierarchy for search, dense/keyword retrieval, RRF, and cross-encoder reranking.
"""
from typing import Optional, Dict, Any


class RetrievalError(Exception):
    """Base exception for all retrieval operations."""

    def __init__(self, message: str, details: Optional[Dict[str, Any]] = None):
        super().__init__(message)
        self.message = message
        self.details = details or {}


class QueryValidationError(RetrievalError):
    """Raised when user search query is invalid, empty, or exceeds constraints."""
    pass


class DenseRetrievalError(RetrievalError):
    """Raised when dense vector retrieval or query embedding fails."""
    pass


class KeywordRetrievalError(RetrievalError):
    """Raised when PostgreSQL full-text search fails."""
    pass


class RerankerError(RetrievalError):
    """Raised when cross-encoder reranking fails or outputs non-finite scores."""
    pass

