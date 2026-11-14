"""
Unit tests for Cross-Encoder Reranker
"""
import uuid
import numpy as np
import pytest
from unittest.mock import MagicMock

from app.retrieval.reranker import CrossEncoderReranker
from app.retrieval.rrf import HybridCandidate
from app.retrieval.exceptions import RerankerError


def make_hybrid_candidate(chunk_id, content="Sample content", rrf=0.01):
    return HybridCandidate(
        chunk_id=chunk_id,
        document_id=uuid.uuid4(),
        document_name="Doc1",
        workspace_id=uuid.uuid4(),
        chunk_content=content,
        page_number=1,
        slide_number=None,
        section_heading="Heading",
        chunk_index=0,
        source_type="PDF",
        rrf_score=rrf,
        dense_rank=1,
        keyword_rank=1,
        dense_similarity=0.85,
        keyword_score=0.4,
        segment_index=0
    )


def test_reranker_empty_candidates():
    reranker = CrossEncoderReranker()
    assert reranker.rerank("query", []) == []


def test_reranker_ordering_with_mock():
    mock_model = MagicMock()
    # Three candidates: scores 0.1, 0.9, 0.5 -> expected order: cand2 (0.9), cand3 (0.5), cand1 (0.1)
    mock_model.predict.return_value = np.array([0.1, 0.9, 0.5], dtype=np.float32)

    reranker = CrossEncoderReranker()
    reranker._model = mock_model

    c1 = make_hybrid_candidate(uuid.uuid4(), "content 1", rrf=0.01)
    c2 = make_hybrid_candidate(uuid.uuid4(), "content 2", rrf=0.02)
    c3 = make_hybrid_candidate(uuid.uuid4(), "content 3", rrf=0.03)

    results = reranker.rerank("search query", [c1, c2, c3], top_k=2)

    assert len(results) == 2
    assert results[0].chunk_id == c2.chunk_id
    assert results[0].final_rank == 1
    assert pytest.approx(results[0].reranker_score) == 0.9

    assert results[1].chunk_id == c3.chunk_id
    assert results[1].final_rank == 2
    assert pytest.approx(results[1].reranker_score) == 0.5


def test_reranker_non_finite_scores():
    mock_model = MagicMock()
    mock_model.predict.return_value = np.array([0.1, float("nan")], dtype=np.float32)

    reranker = CrossEncoderReranker()
    reranker._model = mock_model

    c1 = make_hybrid_candidate(uuid.uuid4())
    c2 = make_hybrid_candidate(uuid.uuid4())

    with pytest.raises(RerankerError) as exc:
        reranker.rerank("query", [c1, c2])
    assert "non-finite" in exc.value.message


def test_reranker_long_passage_handling():
    mock_model = MagicMock()
    mock_model.predict.return_value = np.array([0.8], dtype=np.float32)

    reranker = CrossEncoderReranker()
    reranker._model = mock_model

    long_content = "Computer Science " * 1000
    cand = make_hybrid_candidate(uuid.uuid4(), content=long_content)

    results = reranker.rerank("query", [cand])
    assert len(results) == 1
    assert results[0].final_rank == 1
    # Check that model.predict was invoked with pairs
    assert mock_model.predict.called

