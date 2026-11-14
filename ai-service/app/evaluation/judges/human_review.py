"""
CS Scholar AI - Phase 12.1 Human Review Workflow & Inter-Annotator Agreement
Manages double-blind human evaluation worksheets, 6-dimensional academic rubrics,
and computes Cohen's Kappa inter-annotator agreement between independent reviewers.
"""
from dataclasses import dataclass, field
from typing import List, Dict, Optional, Tuple, Any
from enum import Enum
import math

from app.evaluation.datasets.schemas import EvaluationCase
from app.rag.schemas import TutorAnswerResponse


class HumanJudgmentLabel(str, Enum):
    CORRECT = "CORRECT"
    PARTIALLY_CORRECT = "PARTIALLY_CORRECT"
    INCORRECT = "INCORRECT"
    UNSUPPORTED = "UNSUPPORTED"
    INCORRECT_CITATION = "INCORRECT_CITATION"
    INCOMPLETE = "INCOMPLETE"
    CORRECT_ABSTENTION = "CORRECT_ABSTENTION"
    INCORRECT_ABSTENTION = "INCORRECT_ABSTENTION"


@dataclass
class DimensionalScore:
    """Standardized 1-5 academic rubric rating."""
    factual_correctness: int = 5      # 1 (completely wrong) to 5 (flawlessly accurate)
    citation_accuracy: int = 5        # 1 (fabricated sources) to 5 (precise excerpt support)
    completeness: int = 5             # 1 (major omissions) to 5 (all core concepts covered)
    clarity_and_tone: int = 5         # 1 (incoherent/jargon soup) to 5 (pedagogically lucid)
    hallucination_freedom: int = 5    # 1 (blatant ungrounded claims) to 5 (strictly grounded)
    pedagogical_suitability: int = 5  # 1 (confusing for students) to 5 (conceptually illuminating)


@dataclass
class HumanReviewEntry:
    case_id: str
    subject: str
    question: str
    generated_answer: str
    citations_summary: str
    ground_truth_answer: str
    reviewer_label: Optional[HumanJudgmentLabel] = None
    dimensional_scores: Optional[DimensionalScore] = None
    reviewer_notes: Optional[str] = None


@dataclass
class InterAnnotatorAgreementReport:
    reviewer_1_name: str
    reviewer_2_name: str
    sample_count: int
    raw_agreement_rate: float
    overall_cohens_kappa: float
    kappa_by_dimension: Dict[str, float]
    interpretation: str
    status: str = "PENDING_EXTERNAL_REVIEW"


@dataclass
class HumanReviewConcordanceReport:
    total_reviewed: int
    agreement_count: int
    disagreement_count: int
    concordance_rate: float
    disagreements_by_type: Dict[str, int]
    inter_annotator_agreement: Optional[InterAnnotatorAgreementReport] = None
    status: str = "PENDING_EXTERNAL_REVIEW"


def compute_cohens_kappa(rater1: List[Any], rater2: List[Any]) -> float:
    """
    Computes Cohen's Kappa inter-annotator agreement between two independent raters.
    kappa = (P_o - P_e) / (1 - P_e)
    """
    if len(rater1) != len(rater2) or len(rater1) == 0:
        return 0.0

    n = len(rater1)
    categories = list(set(rater1).union(set(rater2)))

    # Observed agreement P_o
    po = sum(1 for r1, r2 in zip(rater1, rater2) if r1 == r2) / n

    # Expected agreement P_e
    pe = 0.0
    for cat in categories:
        p1 = sum(1 for r in rater1 if r == cat) / n
        p2 = sum(1 for r in rater2 if r == cat) / n
        pe += (p1 * p2)

    if pe >= 1.0:
        return 1.0
    kappa = (po - pe) / (1.0 - pe)
    return max(-1.0, min(1.0, round(float(kappa), 4)))


class HumanReviewWorkflow:
    """Manages human evaluation sheet generation and concordance analysis."""

    @staticmethod
    def generate_review_sheet_markdown(entries: List[HumanReviewEntry]) -> str:
        """Generates a comprehensive Markdown review sheet with double-blind instructions and rubric."""
        lines = [
            "# CS Scholar AI — Academic RAG Double-Blind Human Evaluation Worksheet",
            "",
            "## Evaluation Protocol & Instructions",
            "This worksheet supports independent expert evaluation of AI Academic Tutor answers.",
            "All model metadata, pipeline stage tags, and latency indicators have been omitted to preserve double-blind evaluation integrity.",
            "",
            "### 6-Dimensional Rubric (Score 1 to 5)",
            "1. **Factual Correctness (1-5)**: Is every technical statement rigorously correct according to standard CS textbooks?",
            "2. **Citation Accuracy (1-5)**: Do the cited source tags `[S1]`, `[S2]` directly support the claims they are attached to?",
            "3. **Completeness (1-5)**: Does the answer address all parts of the question, or omit critical conditions/steps?",
            "4. **Clarity & Tone (1-5)**: Is the response lucid, structured, grammatically sound, and pedagogically constructive?",
            "5. **Hallucination Freedom (1-5)**: 5 = zero unsupported or fabricated claims; 1 = multiple fabricated concepts.",
            "6. **Pedagogical Suitability (1-5)**: Would an undergraduate or graduate CS student learn the concept accurately from this answer?",
            "",
            "### Categorical Classification Labels",
            "- `[ ] CORRECT`: Fully accurate, complete, and verified against sources.",
            "- `[ ] PARTIALLY_CORRECT`: Accurate core answer with minor omissions.",
            "- `[ ] INCORRECT`: Contains factual or algorithmic errors.",
            "- `[ ] UNSUPPORTED`: Unsubstantiated claims absent from provided documents.",
            "- `[ ] INCORRECT_CITATION`: Wrong source tag or citation points to unrelated section.",
            "- `[ ] INCOMPLETE`: Missed crucial aspects of the question prompt.",
            "- `[ ] CORRECT_ABSTENTION`: Appropriately refused to hallucinate on unanswerable query.",
            "- `[ ] INCORRECT_ABSTENTION`: Refused to answer despite clear evidence in context.",
            "",
            "---",
            ""
        ]

        for i, entry in enumerate(entries, start=1):
            lines.extend([
                f"### Case {i:02d}: [{entry.case_id}] Subject: {entry.subject}",
                f"**Question:** {entry.question}",
                "",
                f"**Expected Ground Truth:** {entry.ground_truth_answer}",
                "",
                f"**Generated Tutor Answer:**",
                f"> {entry.generated_answer}",
                "",
                f"**Context Citations:** {entry.citations_summary}",
                "",
                "**Categorical Verdict:**",
                "- [ ] CORRECT",
                "- [ ] PARTIALLY_CORRECT",
                "- [ ] INCORRECT",
                "- [ ] UNSUPPORTED",
                "- [ ] INCORRECT_CITATION",
                "- [ ] INCOMPLETE",
                "- [ ] CORRECT_ABSTENTION",
                "- [ ] INCORRECT_ABSTENTION",
                "",
                "**Dimensional Rubric Scores (1 to 5):**",
                "- Factual Correctness (1-5): [ ]",
                "- Citation Accuracy (1-5): [ ]",
                "- Completeness (1-5): [ ]",
                "- Clarity & Tone (1-5): [ ]",
                "- Hallucination Freedom (1-5): [ ]",
                "- Pedagogical Suitability (1-5): [ ]",
                "",
                "**Reviewer Notes / Evidence Analysis:**",
                "____________________________________________________________________________",
                "",
                "---",
                ""
            ])

        return "\n".join(lines)

    @staticmethod
    def compute_concordance(
        human_ratings: Dict[str, HumanJudgmentLabel],
        automated_ratings: Dict[str, str]
    ) -> HumanReviewConcordanceReport:
        """
        Computes agreement between human reviewer classifications and automated pipeline judgments.
        """
        total = len(human_ratings)
        if total == 0:
            return HumanReviewConcordanceReport(0, 0, 0, 1.0, {})

        agreements = 0
        disagreements = 0
        disagreements_by_type: Dict[str, int] = {}

        for case_id, h_label in human_ratings.items():
            a_label = automated_ratings.get(case_id, "")
            is_agree = False

            # Map compatible positive/negative states
            if h_label in [HumanJudgmentLabel.CORRECT, HumanJudgmentLabel.PARTIALLY_CORRECT] and a_label in ["GROUNDED", "PARTIALLY_GROUNDED", "PASS"]:
                is_agree = True
            elif h_label == HumanJudgmentLabel.CORRECT_ABSTENTION and a_label in ["INSUFFICIENT_EVIDENCE", "ABSTAINED"]:
                is_agree = True
            elif h_label in [HumanJudgmentLabel.INCORRECT, HumanJudgmentLabel.UNSUPPORTED] and a_label in ["INSUFFICIENT_EVIDENCE", "FAIL"]:
                is_agree = True

            if is_agree:
                agreements += 1
            else:
                disagreements += 1
                key = f"Human({h_label.value}) vs Auto({a_label})"
                disagreements_by_type[key] = disagreements_by_type.get(key, 0) + 1

        rate = round(float(agreements / total), 4)
        return HumanReviewConcordanceReport(
            total_reviewed=total,
            agreement_count=agreements,
            disagreement_count=disagreements,
            concordance_rate=rate,
            disagreements_by_type=disagreements_by_type
        )

    @staticmethod
    def compute_inter_annotator_agreement(
        reviewer_1: Dict[str, HumanJudgmentLabel],
        reviewer_2: Dict[str, HumanJudgmentLabel],
        reviewer_1_name: str = "Expert Educator 1",
        reviewer_2_name: str = "CS Faculty Reviewer 2",
        dim_scores_1: Optional[Dict[str, Dict[str, int]]] = None,
        dim_scores_2: Optional[Dict[str, Dict[str, int]]] = None,
        is_simulated: bool = False
    ) -> InterAnnotatorAgreementReport:
        """
        Computes inter-annotator agreement and Cohen's Kappa across two independent review streams.
        """
        common_cases = [cid for cid in reviewer_1 if cid in reviewer_2]
        if not common_cases:
            return InterAnnotatorAgreementReport(
                reviewer_1_name=reviewer_1_name,
                reviewer_2_name=reviewer_2_name,
                sample_count=0,
                raw_agreement_rate=1.0,
                overall_cohens_kappa=1.0,
                kappa_by_dimension={},
                interpretation="No overlapping annotations.",
                status="PENDING_EXTERNAL_REVIEW"
            )

        r1_labels = [reviewer_1[cid].value for cid in common_cases]
        r2_labels = [reviewer_2[cid].value for cid in common_cases]
        raw_agree = sum(1 for a, b in zip(r1_labels, r2_labels) if a == b) / len(common_cases)
        kappa = compute_cohens_kappa(r1_labels, r2_labels)

        if kappa >= 0.81:
            interp = "Near-perfect agreement (Landis & Koch standard)"
        elif kappa >= 0.61:
            interp = "Substantial agreement"
        elif kappa >= 0.41:
            interp = "Moderate agreement"
        else:
            interp = "Fair to slight agreement"

        dim_kappas: Dict[str, float] = {}
        if dim_scores_1 and dim_scores_2:
            dim_names = [
                "factual_correctness", "citation_accuracy", "completeness",
                "clarity_and_tone", "hallucination_freedom", "pedagogical_suitability"
            ]
            for dim in dim_names:
                r1_dim = [dim_scores_1[cid].get(dim, 5) for cid in common_cases if cid in dim_scores_1]
                r2_dim = [dim_scores_2[cid].get(dim, 5) for cid in common_cases if cid in dim_scores_2]
                if len(r1_dim) == len(r2_dim) and len(r1_dim) > 0:
                    dim_kappas[dim] = compute_cohens_kappa(r1_dim, r2_dim)

        return InterAnnotatorAgreementReport(
            reviewer_1_name=reviewer_1_name,
            reviewer_2_name=reviewer_2_name,
            sample_count=len(common_cases),
            raw_agreement_rate=round(float(raw_agree), 4),
            overall_cohens_kappa=round(float(kappa), 4),
            kappa_by_dimension=dim_kappas,
            interpretation=interp,
            status="SIMULATED_TEST_FIXTURE" if is_simulated else "PENDING_EXTERNAL_REVIEW"
        )

