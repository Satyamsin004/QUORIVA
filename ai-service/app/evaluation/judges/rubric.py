"""
CS Scholar AI - Phase 12 LLM Judge Scoring Rubrics & Prompts
Defines structured academic evaluation rubrics across Faithfulness, Relevance, Completeness, and Citations.
"""
from dataclasses import dataclass
from typing import Dict, Any


@dataclass
class RubricCriterion:
    name: str
    description: str
    scale_1: str
    scale_2: str
    scale_3: str
    scale_4: str
    scale_5: str


ACADEMIC_EVALUATION_RUBRICS = {
    "faithfulness": RubricCriterion(
        name="Faithfulness to Evidence",
        description="Measures whether all factual claims in the answer are strictly supported by the retrieved context without hallucination.",
        scale_1="Severe hallucination: contains major fabricated claims completely unsupported or contradicted by the evidence.",
        scale_2="Substantially unfaithful: contains several factual assertions not present in the provided context.",
        scale_3="Partially faithful: primary claim is supported, but secondary assertions or technical details lack evidence.",
        scale_4="Mostly faithful: almost all claims are directly grounded with only minor harmless paraphrasing.",
        scale_5="Completely faithful: every single factual claim is directly verifiable from the provided source context."
    ),
    "relevance": RubricCriterion(
        name="Answer Relevance",
        description="Measures how directly and concisely the answer addresses the specific question asked.",
        scale_1="Completely off-topic or evasive: fails to answer the core question.",
        scale_2="Poor relevance: wanders into tangentially related concepts without answering the main inquiry.",
        scale_3="Moderately relevant: answers the general topic but misses the specific nuance or constraint requested.",
        scale_4="Highly relevant: directly addresses the question with minor extraneous detail.",
        scale_5="Exemplary relevance: answers the question directly, concisely, and with precise focus."
    ),
    "completeness": RubricCriterion(
        name="Completeness of Key Facts",
        description="Measures whether the answer covers all essential academic concepts and key facts expected for the topic.",
        scale_1="Grossly incomplete: misses all fundamental concepts and key facts.",
        scale_2="Incomplete: covers only one isolated detail while missing primary academic components.",
        scale_3="Adequate coverage: captures core concept but leaves out important qualifiers or steps.",
        scale_4="Substantially complete: covers nearly all expected key facts with thorough explanations.",
        scale_5="Thoroughly complete: covers every essential fact, invariant, and trade-off expected."
    ),
    "citation_accuracy": RubricCriterion(
        name="Citation Accuracy and Attribution",
        description="Measures whether cited source tags [S1], [S2] point to valid sources that genuinely support the cited sentences.",
        scale_1="Hallucinated citations: cites non-existent source tags or attributes claims to totally unrelated sources.",
        scale_2="Misattributed citations: cites source tags where the content contradicts or does not mention the claimed fact.",
        scale_3="Partially accurate: cites correct general document, but specific claim is not in the cited chunk.",
        scale_4="Accurate citations: almost all inline citations directly point to chunks containing the stated evidence.",
        scale_5="Flawless citations: every factual assertion has an accurate inline citation pointing to authoritative evidence."
    )
}

LLM_JUDGE_SYSTEM_PROMPT = """You are an impartial academic Computer Science evaluation judge.
Your task is to evaluate an AI-generated student tutor answer against a ground-truth question, expected answer, and retrieved source evidence.

Follow this strict evaluation rubric:
1. Faithfulness (1-5): Are all claims supported by the provided source context?
2. Relevance (1-5): Does the answer directly answer the user's question?
3. Completeness (1-5): Are the expected key facts covered?
4. Citation Accuracy (1-5): Do cited [S1], [S2] tags actually contain the cited facts?

Respond ONLY in valid JSON format matching this schema:
{
  "faithfulness_score": <1-5>,
  "relevance_score": <1-5>,
  "completeness_score": <1-5>,
  "citation_accuracy_score": <1-5>,
  "unsupported_claims": ["<list of any claims not supported by evidence>"],
  "hallucinated_citations": ["<list of any hallucinated source tags>"],
  "justification": "<detailed 2-3 sentence justification citing specific evidence>"
}
"""

