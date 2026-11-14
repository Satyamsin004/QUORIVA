"""
CS Scholar AI - Retrieval Pipeline Evaluator
Computes comparative evaluation metrics across retrieval pipeline stages:
Dense-only, Keyword-only, Hybrid RRF, and Hybrid + Cross-Encoder Reranking.
"""
from dataclasses import dataclass
from typing import List, Dict, Any, Set
from uuid import UUID

from app.retrieval.evaluation.metrics import (
    compute_recall_at_k,
    compute_mrr,
    compute_ndcg_at_k
)
from app.retrieval.evaluation.dataset import EvaluationSample


@dataclass
class StageMetrics:
    stage_name: str
    recall_at_5: float
    recall_at_10: float
    mrr: float
    ndcg_at_10: float


class RetrievalEvaluator:
    """
    Evaluates retrieval pipelines against a ground-truth dataset.
    """

    @staticmethod
    def evaluate_stage(
        stage_name: str,
        predictions: Dict[str, List[UUID]],
        dataset: List[EvaluationSample]
    ) -> StageMetrics:
        """
        Evaluate a single pipeline stage over all query samples in the dataset.

        Args:
            stage_name: Name of retrieval stage (e.g. 'Dense Only', 'Hybrid + Reranking')
            predictions: Mapping of query_id -> ordered list of retrieved chunk_ids
            dataset: List of EvaluationSample ground truth entries

        Returns:
            StageMetrics dataclass with average metrics.
        """
        if not dataset:
            return StageMetrics(stage_name, 0.0, 0.0, 0.0, 0.0)

        total_r5 = 0.0
        total_r10 = 0.0
        total_mrr = 0.0
        total_ndcg = 0.0
        n = len(dataset)

        for sample in dataset:
            retrieved = predictions.get(sample.query_id, [])
            relevant = sample.relevant_chunk_ids

            total_r5 += compute_recall_at_k(retrieved, relevant, 5)
            total_r10 += compute_recall_at_k(retrieved, relevant, 10)
            total_mrr += compute_mrr(retrieved, relevant)
            total_ndcg += compute_ndcg_at_k(retrieved, relevant, 10)

        return StageMetrics(
            stage_name=stage_name,
            recall_at_5=round(total_r5 / n, 4),
            recall_at_10=round(total_r10 / n, 4),
            mrr=round(total_mrr / n, 4),
            ndcg_at_10=round(total_ndcg / n, 4)
        )

