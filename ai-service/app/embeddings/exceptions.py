"""
CS Scholar AI - Embedding Exceptions
"""


class EmbeddingError(Exception):
    """Base exception for all embedding generation and persistence errors."""
    def __init__(self, message: str, details: dict = None):
        super().__init__(message)
        self.message = message
        self.details = details or {}


class ModelLoadError(EmbeddingError):
    """Raised when the embedding model or tokenizer fails to load."""
    pass


class InferenceError(EmbeddingError):
    """Raised when embedding model forward pass or inference fails."""
    pass


class DimensionMismatchError(EmbeddingError):
    """Raised when the generated embedding vector does not match the configured dimension."""
    pass


class NormalizationError(EmbeddingError):
    """Raised when embedding normalization fails (NaN, Inf, or zero-vector encountered)."""
    pass


class SegmentationError(EmbeddingError):
    """Raised when chunk text tokenization or segmentation fails."""
    pass


class EmbeddingPersistenceError(EmbeddingError):
    """Raised when storing embeddings in PostgreSQL / pgvector fails."""
    pass

