"""
CS Scholar AI - Phase 12 Generation & Abstention Metrics
Evaluates Answer Correctness, Relevance, Completeness, and Insufficient-Evidence Abstention.
"""
import re
from typing import List, Dict, Any, Tuple, Optional
from dataclasses import dataclass
from app.evaluation.datasets.schemas import EvaluationCase, ExpectedAnswerability
from app.rag.schemas import TutorAnswerResponse, GroundingStatus


@dataclass
class AnswerQualityResult:
    correctness_score: float
    relevance_score: float
    completeness_score: float
    matched_facts: List[str]
    missing_facts: List[str]


@dataclass
class ConfusionMatrix:
    """
    3x3 Grounding and Abstention Confusion Matrix.
    Rows: Actual Case Answerability (from ground-truth corpus & annotations).
    Columns: System Prediction (Grounded, Partially Grounded, Abstained).
    """
    # Actual: Fully Answerable
    fully_ans_grounded: int = 0
    fully_ans_partial: int = 0
    fully_ans_abstained: int = 0  # False Abstention

    # Actual: Partially Answerable
    partial_ans_grounded: int = 0  # Over-grounded
    partial_ans_partial: int = 0   # Correct Partial Grounding
    partial_ans_abstained: int = 0 # Under-grounded / Abstained

    # Actual: Unanswerable (Absent evidence, distractors, missing key facts)
    unans_grounded: int = 0        # Hallucinated / Unsupported Answer
    unans_partial: int = 0         # Hallucinated / Partial Unsupported Answer
    unans_abstained: int = 0       # Correct Abstention

    @property
    def total_fully_answerable(self) -> int:
        return self.fully_ans_grounded + self.fully_ans_partial + self.fully_ans_abstained

    @property
    def total_partially_answerable(self) -> int:
        return self.partial_ans_grounded + self.partial_ans_partial + self.partial_ans_abstained

    @property
    def total_unanswerable(self) -> int:
        return self.unans_grounded + self.unans_partial + self.unans_abstained

    @property
    def total_predicted_grounded(self) -> int:
        return self.fully_ans_grounded + self.partial_ans_grounded + self.unans_grounded

    @property
    def total_predicted_partial(self) -> int:
        return self.fully_ans_partial + self.partial_ans_partial + self.unans_partial

    @property
    def total_predicted_abstained(self) -> int:
        return self.fully_ans_abstained + self.partial_ans_abstained + self.unans_abstained

    @property
    def total_cases(self) -> int:
        return self.total_fully_answerable + self.total_partially_answerable + self.total_unanswerable

    def to_dict(self) -> Dict[str, Any]:
        return {
            "matrix": {
                "fully_answerable": {
                    "grounded": self.fully_ans_grounded,
                    "partially_grounded": self.fully_ans_partial,
                    "abstained": self.fully_ans_abstained,
                    "total": self.total_fully_answerable,
                },
                "partially_answerable": {
                    "grounded": self.partial_ans_grounded,
                    "partially_grounded": self.partial_ans_partial,
                    "abstained": self.partial_ans_abstained,
                    "total": self.total_partially_answerable,
                },
                "unanswerable": {
                    "grounded": self.unans_grounded,
                    "partially_grounded": self.unans_partial,
                    "abstained": self.unans_abstained,
                    "total": self.total_unanswerable,
                },
            },
            "predicted_totals": {
                "grounded": self.total_predicted_grounded,
                "partially_grounded": self.total_predicted_partial,
                "abstained": self.total_predicted_abstained,
            },
            "total_cases": self.total_cases,
        }


@dataclass
class AbstentionMetricsReport:
    total_unanswerable: int
    correct_abstentions: int
    unsupported_answers: int
    total_answerable: int
    correct_answers: int
    false_abstentions: int
    total_partial: int
    correct_partial_grounding: int
    correct_abstention_rate: float
    false_abstention_rate: float
    unsupported_answer_rate: float
    partial_grounding_accuracy: float
    confusion_matrix: Optional[ConfusionMatrix] = None
    grounding_accuracy: float = 1.0


def compute_fact_coverage(answer: str, key_facts: List[str]) -> Tuple[float, List[str], List[str]]:
    """
    Computes key fact coverage in the generated answer based on semantic keyword presence.
    Accounts for inflectional variants (plurals, verb tenses).
    """
    if not key_facts:
        return 1.0, [], []

    answer_lower = answer.lower()
    matched = []
    missing = []

    for fact in key_facts:
        fact_words = [w for w in re.findall(r'\b[a-zA-Z]{4,}\b', fact.lower())]
        if not fact_words:
            matched.append(fact)
            continue

        match_count = sum(
            1 for w in fact_words
            if w in answer_lower
            or (len(w) > 4 and w.rstrip('s') in answer_lower)
            or (len(w) > 4 and w.rstrip('ed') in answer_lower)
            or (len(w) > 4 and w.rstrip('ing') in answer_lower)
        )
        if match_count / len(fact_words) >= 0.45:
            matched.append(fact)
        else:
            missing.append(fact)

    coverage = float(len(matched) / len(key_facts))
    return coverage, matched, missing


def evaluate_answer_quality(answer: str, case: EvaluationCase) -> AnswerQualityResult:
    """
    Evaluates correctness, relevance, and completeness against ground-truth facts.
    """
    # If the case is unanswerable from the provided corpus, a correct abstention is fully correct and relevant
    if case.answerability == ExpectedAnswerability.UNANSWERABLE:
        is_abstained = any(phrase in answer.lower() for phrase in [
            "insufficient evidence",
            "cannot be answered",
            "not contain",
            "not provided",
            "no information",
            "do not contain"
        ])
        if is_abstained:
            return AnswerQualityResult(
                correctness_score=1.0,
                relevance_score=1.0,
                completeness_score=1.0,
                matched_facts=case.key_facts,
                missing_facts=[]
            )
        else:
            return AnswerQualityResult(
                correctness_score=0.0,
                relevance_score=0.5,
                completeness_score=0.0,
                matched_facts=[],
                missing_facts=case.key_facts
            )

    coverage, matched, missing = compute_fact_coverage(answer, case.key_facts)

    # Relevance: Question keyword presence in answer
    q_words = [w for w in re.findall(r'\b[a-zA-Z]{4,}\b', case.question.lower())]
    if q_words:
        ans_lower = answer.lower()
        q_hits = sum(1 for w in q_words if w in ans_lower)
        relevance = min(1.0, float(q_hits / len(q_words)) * 1.2)
    else:
        relevance = 1.0

    return AnswerQualityResult(
        correctness_score=round(coverage, 4),
        relevance_score=round(relevance, 4),
        completeness_score=round(coverage, 4),
        matched_facts=matched,
        missing_facts=missing
    )


def compute_abstention_metrics(
    evaluations: List[Tuple[EvaluationCase, TutorAnswerResponse]]
) -> AbstentionMetricsReport:
    """
    Computes correct abstention rate, false abstention rate, unsupported answer rate,
    grounding accuracy, and populates the 3x3 confusion matrix.
    """
    matrix = ConfusionMatrix()

    for case, resp in evaluations:
        if resp.grounding_status == GroundingStatus.PARTIALLY_GROUNDED:
            predicted = "PARTIALLY_GROUNDED"
        elif (
            resp.insufficient_evidence or
            resp.grounding_status == GroundingStatus.INSUFFICIENT_EVIDENCE or
            any(phrase in resp.answer.lower() for phrase in [
                "insufficient evidence",
                "cannot be answered"
            ])
        ):
            predicted = "ABSTAINED"
        else:
            predicted = "GROUNDED"

        if case.answerability == ExpectedAnswerability.UNANSWERABLE:
            if predicted == "ABSTAINED":
                matrix.unans_abstained += 1
            elif predicted == "PARTIALLY_GROUNDED":
                matrix.unans_partial += 1
            else:
                matrix.unans_grounded += 1

        elif case.answerability == ExpectedAnswerability.ANSWERABLE:
            if predicted == "GROUNDED":
                matrix.fully_ans_grounded += 1
            elif predicted == "PARTIALLY_GROUNDED":
                matrix.fully_ans_partial += 1
            else:
                matrix.fully_ans_abstained += 1

        elif case.answerability == ExpectedAnswerability.PARTIALLY_ANSWERABLE:
            if predicted == "PARTIALLY_GROUNDED":
                matrix.partial_ans_partial += 1
            elif predicted == "GROUNDED":
                matrix.partial_ans_grounded += 1
            else:
                matrix.partial_ans_abstained += 1

    total_unans = matrix.total_unanswerable
    total_fully_ans = matrix.total_fully_answerable
    total_partial_ans = matrix.total_partially_answerable

    # Correct Abstention Rate: correct abstentions / total unanswerable cases
    correct_abstention_rate = (
        float(matrix.unans_abstained / total_unans) if total_unans > 0 else 1.0
    )
    # Unsupported Answer Rate: (hallucinated grounded + hallucinated partial) / total unanswerable cases
    unsupported_answer_rate = (
        float((matrix.unans_grounded + matrix.unans_partial) / total_unans) if total_unans > 0 else 0.0
    )
    # False Abstention Rate: false abstentions / total answerable cases
    false_abstention_rate = (
        float(matrix.fully_ans_abstained / total_fully_ans) if total_fully_ans > 0 else 0.0
    )
    # Partial Grounding Accuracy: correct partial / total partial cases
    partial_accuracy = (
        float(matrix.partial_ans_partial / total_partial_ans) if total_partial_ans > 0 else 1.0
    )
    # Fully Grounded Accuracy: correct grounded / total answerable cases
    grounding_accuracy = (
        float(matrix.fully_ans_grounded / total_fully_ans) if total_fully_ans > 0 else 1.0
    )

    return AbstentionMetricsReport(
        total_unanswerable=total_unans,
        correct_abstentions=matrix.unans_abstained,
        unsupported_answers=matrix.unans_grounded + matrix.unans_partial,
        total_answerable=total_fully_ans,
        correct_answers=matrix.fully_ans_grounded,
        false_abstentions=matrix.fully_ans_abstained,
        total_partial=total_partial_ans,
        correct_partial_grounding=matrix.partial_ans_partial,
        correct_abstention_rate=round(correct_abstention_rate, 4),
        false_abstention_rate=round(false_abstention_rate, 4),
        unsupported_answer_rate=round(unsupported_answer_rate, 4),
        partial_grounding_accuracy=round(partial_accuracy, 4),
        confusion_matrix=matrix,
        grounding_accuracy=round(grounding_accuracy, 4)
    )

