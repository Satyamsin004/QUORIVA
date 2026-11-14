"""
CS Scholar AI - Phase 12 Multi-Stage Retrieval Evaluator
Computes comprehensive retrieval metrics (Recall@1/3/5/10, MRR, nDCG@5/10, Precision@5)
across all retrieval stages with breakdown by Subject, Difficulty, Category, and Multi-Source.
"""
from dataclasses import dataclass, field
from typing import List, Dict, Set, Any, Optional
from uuid import UUID

from app.evaluation.datasets.schemas import (
    EvaluationCase,
    CorpusChunk,
    DifficultyLevel,
    QuestionCategory
)
from app.evaluation.metrics.retrieval import (
    compute_recall_at_k,
    compute_precision_at_k,
    compute_mrr,
    compute_ndcg_at_k
)


@dataclass
class StageRetrievalMetrics:
    stage_name: str
    sample_count: int
    recall_at_1: float
    recall_at_3: float
    recall_at_5: float
    recall_at_10: float
    mrr: float
    ndcg_at_5: float
    ndcg_at_10: float
    precision_at_5: float
    average_candidate_count: float = 0.0
    latency_ms: float = 0.0


@dataclass
class SliceMetricsReport:
    slice_type: str  # e.g., "subject", "difficulty", "category", "multi_source"
    slice_name: str
    sample_count: int
    metrics_by_stage: Dict[str, StageRetrievalMetrics]


@dataclass
class FullMultiStageReport:
    overall_by_stage: Dict[str, StageRetrievalMetrics]
    by_subject: Dict[str, Dict[str, StageRetrievalMetrics]]
    by_difficulty: Dict[str, Dict[str, StageRetrievalMetrics]]
    by_category: Dict[str, Dict[str, StageRetrievalMetrics]]
    by_multi_source: Dict[str, Dict[str, StageRetrievalMetrics]]


class MultiStageRetrievalEvaluator:
    """Evaluates multi-stage retrieval pipelines against ground truth benchmark cases."""

    @staticmethod
    def evaluate_stage_subset(
        stage_name: str,
        predictions: Dict[str, List[UUID]],
        cases: List[EvaluationCase],
        latency_ms: float = 0.0
    ) -> StageRetrievalMetrics:
        """Computes metrics for a single stage across a given list of evaluation cases."""
        # Filter for cases that have relevant chunks (answerable)
        eval_cases = [c for c in cases if c.relevant_chunk_ids]
        n = len(eval_cases)

        if n == 0:
            return StageRetrievalMetrics(
                stage_name=stage_name,
                sample_count=0,
                recall_at_1=0.0,
                recall_at_3=0.0,
                recall_at_5=0.0,
                recall_at_10=0.0,
                mrr=0.0,
                ndcg_at_5=0.0,
                ndcg_at_10=0.0,
                precision_at_5=0.0,
                latency_ms=latency_ms
            )

        sum_r1 = 0.0
        sum_r3 = 0.0
        sum_r5 = 0.0
        sum_r10 = 0.0
        sum_mrr = 0.0
        sum_ndcg5 = 0.0
        sum_ndcg10 = 0.0
        sum_p5 = 0.0
        total_candidates = 0

        for case in eval_cases:
            preds = predictions.get(case.case_id, [])
            total_candidates += len(preds)
            relevant_set = set(case.relevant_chunk_ids)

            sum_r1 += compute_recall_at_k(preds, relevant_set, 1)
            sum_r3 += compute_recall_at_k(preds, relevant_set, 3)
            sum_r5 += compute_recall_at_k(preds, relevant_set, 5)
            sum_r10 += compute_recall_at_k(preds, relevant_set, 10)
            sum_mrr += compute_mrr(preds, relevant_set)
            sum_ndcg5 += compute_ndcg_at_k(preds, relevant_set, 5, case.graded_relevance)
            sum_ndcg10 += compute_ndcg_at_k(preds, relevant_set, 10, case.graded_relevance)
            sum_p5 += compute_precision_at_k(preds, relevant_set, 5)

        return StageRetrievalMetrics(
            stage_name=stage_name,
            sample_count=n,
            recall_at_1=round(sum_r1 / n, 4),
            recall_at_3=round(sum_r3 / n, 4),
            recall_at_5=round(sum_r5 / n, 4),
            recall_at_10=round(sum_r10 / n, 4),
            mrr=round(sum_mrr / n, 4),
            ndcg_at_5=round(sum_ndcg5 / n, 4),
            ndcg_at_10=round(sum_ndcg10 / n, 4),
            precision_at_5=round(sum_p5 / n, 4),
            average_candidate_count=round(total_candidates / n, 1),
            latency_ms=round(latency_ms, 2)
        )

    def evaluate_all_stages(
        self,
        stage_predictions: Dict[str, Dict[str, List[UUID]]],
        cases: List[EvaluationCase],
        stage_latencies: Optional[Dict[str, float]] = None
    ) -> FullMultiStageReport:
        """
        Computes overall and sliced metrics across all stages:
          Dense Only, Keyword Only, Hybrid pre-RRF, Hybrid RRF, Hybrid + Reranking.
        """
        latencies = stage_latencies or {}
        overall: Dict[str, StageRetrievalMetrics] = {}

        for stage, preds in stage_predictions.items():
            latency = latencies.get(stage, 0.0)
            overall[stage] = self.evaluate_stage_subset(stage, preds, cases, latency)

        # Slices: By Subject
        by_subject: Dict[str, Dict[str, StageRetrievalMetrics]] = {}
        subjects = sorted(list({c.subject for c in cases}))
        for subj in subjects:
            subj_cases = [c for c in cases if c.subject == subj]
            by_subject[subj] = {
                stage: self.evaluate_stage_subset(stage, preds, subj_cases, latencies.get(stage, 0.0))
                for stage, preds in stage_predictions.items()
            }

        # Slices: By Difficulty
        by_difficulty: Dict[str, Dict[str, StageRetrievalMetrics]] = {}
        for diff in DifficultyLevel:
            diff_cases = [c for c in cases if c.difficulty == diff]
            if diff_cases:
                by_difficulty[diff.value] = {
                    stage: self.evaluate_stage_subset(stage, preds, diff_cases, latencies.get(stage, 0.0))
                    for stage, preds in stage_predictions.items()
                }

        # Slices: By Question Category
        by_category: Dict[str, Dict[str, StageRetrievalMetrics]] = {}
        for cat in QuestionCategory:
            cat_cases = [c for c in cases if c.category == cat]
            if cat_cases:
                by_category[cat.value] = {
                    stage: self.evaluate_stage_subset(stage, preds, cat_cases, latencies.get(stage, 0.0))
                    for stage, preds in stage_predictions.items()
                }

        # Slices: Single vs Multi Source
        by_multi_source: Dict[str, Dict[str, StageRetrievalMetrics]] = {
            "Single-Source": {
                stage: self.evaluate_stage_subset(stage, preds, [c for c in cases if not c.is_multi_source], latencies.get(stage, 0.0))
                for stage, preds in stage_predictions.items()
            },
            "Multi-Source": {
                stage: self.evaluate_stage_subset(stage, preds, [c for c in cases if c.is_multi_source], latencies.get(stage, 0.0))
                for stage, preds in stage_predictions.items()
            }
        }

        return FullMultiStageReport(
            overall_by_stage=overall,
            by_subject=by_subject,
            by_difficulty=by_difficulty,
            by_category=by_category,
            by_multi_source=by_multi_source
        )

