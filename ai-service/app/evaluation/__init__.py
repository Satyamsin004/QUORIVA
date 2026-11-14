"""
CS Scholar AI - Phase 12 RAG Evaluation & Benchmarking Package
"""
from app.evaluation.datasets.schemas import (
    CorpusChunk,
    EvaluationCase,
    QuestionCategory,
    DifficultyLevel,
    ExpectedAnswerability,
    DatasetSplit
)
from app.evaluation.datasets.loader import BenchmarkDataLoader
from app.evaluation.metrics.retrieval import (
    compute_recall_at_k,
    compute_precision_at_k,
    compute_mrr,
    compute_ndcg_at_k
)
from app.evaluation.metrics.citations import (
    ClaimVerificationResult,
    CitationEvaluationReport,
    evaluate_citations_and_claims
)
from app.evaluation.metrics.generation import (
    AnswerQualityResult,
    ConfusionMatrix,
    AbstentionMetricsReport,
    evaluate_answer_quality,
    compute_abstention_metrics
)
from app.evaluation.metrics.learning import (
    QuizQualityReport,
    FlashcardQualityReport,
    evaluate_quiz_quality,
    evaluate_flashcard_quality
)
from app.evaluation.retrieval.multi_stage import MultiStageRetrievalEvaluator
from app.evaluation.retrieval.ablation import RetrievalAblationRunner
from app.evaluation.generation.rag_evaluator import RAGAnswerEvaluator
from app.evaluation.generation.abstention import AbstentionBenchmarkRunner
from app.evaluation.judges.llm_judge import LLMJudge
from app.evaluation.judges.human_review import HumanReviewWorkflow
from app.evaluation.performance.benchmark import PipelinePerformanceBenchmark
from app.evaluation.reports.generator import BenchmarkReportGenerator

__all__ = [
    "CorpusChunk",
    "EvaluationCase",
    "QuestionCategory",
    "DifficultyLevel",
    "ExpectedAnswerability",
    "DatasetSplit",
    "BenchmarkDataLoader",
    "compute_recall_at_k",
    "compute_precision_at_k",
    "compute_mrr",
    "compute_ndcg_at_k",
    "ClaimVerificationResult",
    "CitationEvaluationReport",
    "evaluate_citations_and_claims",
    "AnswerQualityResult",
    "ConfusionMatrix",
    "AbstentionMetricsReport",
    "evaluate_answer_quality",
    "compute_abstention_metrics",
    "QuizQualityReport",
    "FlashcardQualityReport",
    "evaluate_quiz_quality",
    "evaluate_flashcard_quality",
    "MultiStageRetrievalEvaluator",
    "RetrievalAblationRunner",
    "RAGAnswerEvaluator",
    "AbstentionBenchmarkRunner",
    "LLMJudge",
    "HumanReviewWorkflow",
    "PipelinePerformanceBenchmark",
    "BenchmarkReportGenerator"
]
