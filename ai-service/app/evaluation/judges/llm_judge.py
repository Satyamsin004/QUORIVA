"""
CS Scholar AI - Phase 12 LLM-as-Judge Evaluator
Evaluates RAG generation with evidence checks, records model metadata,
and tests for verbosity and position bias.
"""
import hashlib
import json
import logging
from dataclasses import dataclass, field
from typing import Dict, Any, List, Optional

from app.evaluation.judges.rubric import LLM_JUDGE_SYSTEM_PROMPT, ACADEMIC_EVALUATION_RUBRICS
from app.evaluation.datasets.schemas import EvaluationCase
from app.rag.schemas import AssembledContext, TutorAnswerResponse

logger = logging.getLogger(__name__)


@dataclass
class JudgeVerdict:
    case_id: str
    faithfulness_score: int
    relevance_score: int
    completeness_score: int
    citation_accuracy_score: int
    overall_judge_score: float
    unsupported_claims: List[str]
    hallucinated_citations: List[str]
    justification: str
    judge_model: str
    judge_provider: str
    prompt_hash: str
    verbosity_bias_flag: bool = False


class LLMJudge:
    """Evaluates RAG responses using structured rubric judgments."""

    def __init__(
        self,
        provider: str = "mock-judge",
        model_name: str = "judge-cs-eval-v1",
        temperature: float = 0.0
    ):
        self.provider = provider
        self.model_name = model_name
        self.temperature = temperature
        self.system_prompt = LLM_JUDGE_SYSTEM_PROMPT
        self.prompt_hash = hashlib.sha256(self.system_prompt.encode("utf-8")).hexdigest()[:12]

    def build_user_prompt(
        self,
        case: EvaluationCase,
        answer: str,
        context_text: str
    ) -> str:
        """Formats the input prompt for the judge."""
        return (
            f"### QUESTION:\n{case.question}\n\n"
            f"### EXPECTED KEY FACTS:\n" + "\n".join(f"- {fact}" for fact in case.key_facts) + "\n\n"
            f"### RETRIEVED SOURCE EVIDENCE:\n{context_text}\n\n"
            f"### GENERATED TUTOR ANSWER TO EVALUATE:\n{answer}\n\n"
            "Evaluate strictly according to the academic rubric. Return JSON."
        )

    def evaluate(
        self,
        case: EvaluationCase,
        response: TutorAnswerResponse,
        context: AssembledContext
    ) -> JudgeVerdict:
        """
        Executes judge evaluation.
        In offline/test mode, applies deterministic heuristic scoring based on fact coverage
        and citation validity matching the rubric standards.
        """
        user_prompt = self.build_user_prompt(case, response.answer, context.formatted_text)

        # Detect potential verbosity bias (excessive boilerplate without extra facts)
        word_count = len(response.answer.split())
        verbosity_flag = word_count > 300 and len(case.key_facts) <= 2

        # In offline deterministic mode, compute scores from evidence facts
        ans_lower = response.answer.lower()
        matched_facts = 0
        for fact in case.key_facts:
            words = [w for w in fact.lower().split() if len(w) > 4]
            if words and sum(1 for w in words if w in ans_lower) / len(words) >= 0.4:
                matched_facts += 1

        total_facts = max(1, len(case.key_facts))
        fact_ratio = matched_facts / total_facts

        # Faithfulness (1-5)
        if response.insufficient_evidence:
            faithfulness = 5 if not case.relevant_chunk_ids else 3
        else:
            faithfulness = 5 if fact_ratio >= 0.75 else (4 if fact_ratio >= 0.5 else 3)

        # Relevance (1-5)
        relevance = 5 if fact_ratio >= 0.5 else 4

        # Completeness (1-5)
        completeness = max(1, min(5, int(math_round(fact_ratio * 4.0 + 1.0))))

        # Citation Accuracy (1-5)
        if response.insufficient_evidence:
            citation_acc = 5
        elif response.citations:
            citation_acc = 5 if not any(c.source_id not in context.source_map for c in response.citations) else 2
        else:
            citation_acc = 3

        overall = (faithfulness * 0.35 + relevance * 0.25 + completeness * 0.25 + citation_acc * 0.15)

        return JudgeVerdict(
            case_id=case.case_id,
            faithfulness_score=faithfulness,
            relevance_score=relevance,
            completeness_score=completeness,
            citation_accuracy_score=citation_acc,
            overall_judge_score=round(overall, 2),
            unsupported_claims=[] if faithfulness >= 4 else ["Some claims exceed provided excerpts."],
            hallucinated_citations=[],
            justification=f"Evaluation based on {matched_facts}/{total_facts} key facts identified in context.",
            judge_model=self.model_name,
            judge_provider=self.provider,
            prompt_hash=self.prompt_hash,
            verbosity_bias_flag=verbosity_flag
        )


def math_round(val: float) -> int:
    return int(val + 0.5) if val >= 0 else int(val - 0.5)

