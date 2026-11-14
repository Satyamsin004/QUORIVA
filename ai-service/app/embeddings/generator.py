"""
CS Scholar AI - Embedding Vector Generator
Performs batched inference, L2 normalization, numerical verification, and quality checks.
"""
import logging
from typing import List, Optional
import numpy as np
import torch

from app.embeddings.config import embedding_config
from app.embeddings.model import EmbeddingModel
from app.embeddings.exceptions import (
    InferenceError,
    DimensionMismatchError,
    NormalizationError
)

logger = logging.getLogger(__name__)


class EmbeddingGenerator:
    """
    Batched embedding generator for document segments.
    Applies L2 normalization and rigorous numerical quality verification.
    """

    def __init__(
        self,
        model_loader: Optional[EmbeddingModel] = None,
        expected_dimension: Optional[int] = None,
        batch_size: Optional[int] = None
    ):
        self.model_loader = model_loader or EmbeddingModel.get_instance()
        self.expected_dimension = expected_dimension or embedding_config.dimension
        self.batch_size = batch_size or embedding_config.batch_size

    def validate_and_normalize(self, vector: np.ndarray) -> np.ndarray:
        """
        Validate vector shape and finite values, then apply unit L2 normalization.
        
        Args:
            vector: 1D numpy array representing an embedding.
            
        Returns:
            Normalized 1D numpy array of float32.
            
        Raises:
            DimensionMismatchError: If dimension is not 768.
            NormalizationError: If vector contains NaN, Inf, or has zero norm.
        """
        if vector.shape != (self.expected_dimension,):
            raise DimensionMismatchError(
                f"Embedding dimension {vector.shape[0]} does not match expected {self.expected_dimension}.",
                {"actual": vector.shape[0], "expected": self.expected_dimension}
            )

        if not np.all(np.isfinite(vector)):
            raise NormalizationError(
                "Embedding vector contains non-finite numerical values (NaN or Inf).",
                {"has_nan": bool(np.isnan(vector).any()), "has_inf": bool(np.isinf(vector).any())}
            )

        norm = np.linalg.norm(vector)
        if norm < 1e-8:
            raise NormalizationError("Cannot normalize a near-zero or zero magnitude vector.", {"norm": float(norm)})

        normalized = vector / norm

        # Verify unit length
        unit_norm = np.linalg.norm(normalized)
        if abs(unit_norm - 1.0) > 1e-3:
            raise NormalizationError(f"Vector failed unit norm check after normalization: {unit_norm}")

        return normalized.astype(np.float32)

    def generate_embeddings(self, texts: List[str]) -> List[np.ndarray]:
        """
        Generate L2-normalized 768-dimensional embeddings for a list of text strings in batches.
        
        Args:
            texts: List of text strings.
            
        Returns:
            List of normalized numpy arrays of shape (768,).
        """
        if not texts:
            return []

        model = self.model_loader.get_model()

        try:
            with torch.inference_mode():
                # sentence-transformers encode with normalize_embeddings=True
                raw_embeddings = model.encode(
                    texts,
                    batch_size=self.batch_size,
                    show_progress_bar=False,
                    convert_to_numpy=True,
                    normalize_embeddings=True
                )
        except Exception as e:
            logger.error("Embedding generation failed during model forward pass: %s", e, exc_info=True)
            raise InferenceError(f"Model inference failed: {e}", {"text_count": len(texts)})

        if not np.all(np.isfinite(raw_embeddings)):
            raise NormalizationError("Embedding vector contains non-finite numerical values (NaN or Inf).")

        return [vec.astype(np.float32) for vec in raw_embeddings]

