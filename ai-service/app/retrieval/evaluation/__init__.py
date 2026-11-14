"""
CS Scholar AI - Retrieval Evaluation Package
"""
from app.retrieval.evaluation.metrics import (
    compute_recall_at_k,
    compute_mrr,
    compute_ndcg_at_k
)
from app.retrieval.evaluation.dataset import (
    EvaluationSample,
    CS_BENCHMARK_SAMPLES,
    SYNTHETIC_CHUNKS
)
from app.retrieval.evaluation.evaluator import (
    RetrievalEvaluator,
    StageMetrics
)

__all__ = [
    "compute_recall_at_k",
    "compute_mrr",
    "compute_ndcg_at_k",
    "EvaluationSample",
    "CS_BENCHMARK_SAMPLES",
    "SYNTHETIC_CHUNKS",
    "RetrievalEvaluator",
    "StageMetrics",
]

