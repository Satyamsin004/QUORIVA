"""
CS Scholar AI - Embedding Configuration
"""
from dataclasses import dataclass
from app.core.config import settings


@dataclass(frozen=True)
class EmbeddingConfig:
    """Configuration for embedding models, dimensions, token lengths, and batching."""
    model_name: str = settings.embedding_model_name
    model_version: str = settings.embedding_model_version
    dimension: int = settings.embedding_dimension
    batch_size: int = settings.embedding_batch_size
    device: str = settings.embedding_device
    max_sequence_length: int = settings.embedding_max_sequence_length
    # Reserve 2 tokens for [CLS] and [SEP]
    max_content_tokens: int = settings.embedding_max_sequence_length - 2
    segment_overlap_tokens: int = 40


embedding_config = EmbeddingConfig()

