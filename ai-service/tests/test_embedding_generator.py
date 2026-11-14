"""
CS Scholar AI - Embedding Vector Generator Unit Tests
"""
from unittest.mock import MagicMock
import numpy as np
import pytest

from app.embeddings.generator import EmbeddingGenerator
from app.embeddings.exceptions import DimensionMismatchError, NormalizationError


def test_validate_and_normalize_valid_vector():
    generator = EmbeddingGenerator(expected_dimension=768)
    raw = np.random.randn(768).astype(np.float32)
    normalized = generator.validate_and_normalize(raw)

    assert normalized.shape == (768,)
    norm = np.linalg.norm(normalized)
    assert abs(norm - 1.0) < 1e-3


def test_reject_dimension_mismatch():
    generator = EmbeddingGenerator(expected_dimension=768)
    wrong_dim = np.random.randn(512).astype(np.float32)
    with pytest.raises(DimensionMismatchError):
        generator.validate_and_normalize(wrong_dim)


def test_reject_nan_values():
    generator = EmbeddingGenerator(expected_dimension=768)
    nan_vector = np.random.randn(768).astype(np.float32)
    nan_vector[10] = np.nan
    with pytest.raises(NormalizationError):
        generator.validate_and_normalize(nan_vector)


def test_reject_inf_values():
    generator = EmbeddingGenerator(expected_dimension=768)
    inf_vector = np.random.randn(768).astype(np.float32)
    inf_vector[42] = np.inf
    with pytest.raises(NormalizationError):
        generator.validate_and_normalize(inf_vector)


def test_reject_zero_vector():
    generator = EmbeddingGenerator(expected_dimension=768)
    zero_vector = np.zeros(768, dtype=np.float32)
    with pytest.raises(NormalizationError):
        generator.validate_and_normalize(zero_vector)


def test_generate_embeddings_batched_mock():
    mock_model = MagicMock()
    # Mock encode returning 2 vectors of dim 768
    vec1 = np.ones(768, dtype=np.float32) / np.sqrt(768)
    vec2 = np.ones(768, dtype=np.float32) * 2 / np.sqrt(768 * 4)
    mock_model.encode.return_value = np.array([vec1, vec2])

    mock_loader = MagicMock()
    mock_loader.get_model.return_value = mock_model

    generator = EmbeddingGenerator(model_loader=mock_loader, expected_dimension=768, batch_size=2)
    results = generator.generate_embeddings(["text 1", "text 2"])

    assert len(results) == 2
    assert results[0].shape == (768,)
    assert abs(np.linalg.norm(results[0]) - 1.0) < 1e-3
    assert abs(np.linalg.norm(results[1]) - 1.0) < 1e-3

