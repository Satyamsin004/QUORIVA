"""
CS Scholar AI - RAG Exceptions
"""


class RAGError(Exception):
    """Base exception for RAG pipeline errors."""
    pass


class InsufficientEvidenceError(RAGError):
    """Raised when retrieved material lacks sufficient evidence to answer the query."""
    pass


class CitationValidationError(RAGError):
    """Raised when citation validation fails critically."""
    pass

