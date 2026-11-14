"""
Unit tests for Retrieval Evaluation Metrics and Evaluator
"""
import uuid
import pytest
from app.retrieval.evaluation.metrics import (
    compute_recall_at_k,
    compute_mrr,
    compute_ndcg_at_k
)
from app.retrieval.evaluation.evaluator import RetrievalEvaluator
from app.retrieval.evaluation.dataset import EvaluationSample


def test_recall_at_k():
    c1, c2, c3, c4 = uuid.uuid4(), uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
    relevant = {c1, c2}

    # Top 2 has c1 -> 1/2 = 0.5
    assert compute_recall_at_k([c1, c3, c4], relevant, k=2) == 0.5
    # Top 3 has c1 and c2 -> 2/2 = 1.0
    assert compute_recall_at_k([c1, c3, c2, c4], relevant, k=3) == 1.0
    # No relevant
    assert compute_recall_at_k([c3, c4], relevant, k=2) == 0.0


def test_mrr():
    c1, c2, c3 = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
    relevant = {c1}

    # First item relevant -> 1/1 = 1.0
    assert compute_mrr([c1, c2, c3], relevant) == 1.0
    # Second item relevant -> 1/2 = 0.5
    assert compute_mrr([c2, c1, c3], relevant) == 0.5
    # Third item relevant -> 1/3 ≈ 0.3333
    assert pytest.approx(compute_mrr([c2, c3, c1], relevant), rel=1e-3) == 1.0 / 3.0
    # Not found -> 0.0
    assert compute_mrr([c2, c3], relevant) == 0.0


def test_ndcg_at_k():
    c1, c2, c3 = uuid.uuid4(), uuid.uuid4(), uuid.uuid4()
    relevant = {c1, c2}

    # Ideal ranking: [c1, c2, c3] -> nDCG@3 = 1.0
    assert compute_ndcg_at_k([c1, c2, c3], relevant, k=3) == 1.0

    # Suboptimal ranking: [c3, c1, c2] -> DCG < IDCG -> nDCG < 1.0
    ndcg = compute_ndcg_at_k([c3, c1, c2], relevant, k=3)
    assert 0.0 < ndcg < 1.0


def test_retrieval_evaluator_stage():
    q1 = uuid.uuid4()
    c1, c2 = uuid.uuid4(), uuid.uuid4()

    sample = EvaluationSample(
        query_id="q1",
        query="test query",
        topic="test",
        relevant_chunk_ids={c1}
    )

    preds = {"q1": [c1, c2]}
    metrics = RetrievalEvaluator.evaluate_stage("Hybrid RRF", preds, [sample])

    assert metrics.stage_name == "Hybrid RRF"
    assert metrics.recall_at_5 == 1.0
    assert metrics.mrr == 1.0
    assert metrics.ndcg_at_10 == 1.0

