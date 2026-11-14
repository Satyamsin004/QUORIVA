"""
CS Scholar AI - Phase 12 RAG Answer Quality Evaluator
Evaluates the Academic Tutor generation pipeline across correctness, relevance,
completeness, citation precision, citation coverage, claim-level faithfulness, and hallucination rate.
"""
from dataclasses import dataclass
from typing import List, Dict, Optional, Tuple
from uuid import UUID

from app.evaluation.datasets.schemas import EvaluationCase, CorpusChunk
from app.evaluation.metrics.citations import (
    CitationEvaluationReport,
    evaluate_citations_and_claims
)
from app.evaluation.metrics.generation import (
    AnswerQualityResult,
    evaluate_answer_quality
)
from app.rag.schemas import (
    AssembledContext,
    RetrievedContextChunk,
    TutorAnswerResponse,
    GroundingStatus
)


@dataclass
class CaseGenerationEvaluation:
    case_id: str
    subject: str
    question: str
    generated_answer: str
    grounding_status: GroundingStatus
    insufficient_evidence: bool
    correctness_score: float
    relevance_score: float
    completeness_score: float
    citation_precision: float
    citation_coverage: float
    claim_faithfulness: float
    hallucination_rate: float
    source_id_validity_rate: float
    matched_facts: List[str]
    missing_facts: List[str]


@dataclass
class AggregateGenerationReport:
    sample_count: int
    mean_correctness: float
    mean_relevance: float
    mean_completeness: float
    mean_citation_precision: float
    mean_citation_coverage: float
    mean_claim_faithfulness: float
    mean_hallucination_rate: float
    mean_source_id_validity: float
    grounded_count: int
    partially_grounded_count: int
    insufficient_evidence_count: int
    case_evaluations: List[CaseGenerationEvaluation]


class RAGAnswerEvaluator:
    """Evaluates RAG-generated academic tutor answers against ground-truth facts and evidence."""

    def __init__(self, corpus_map: Dict[UUID, CorpusChunk]):
        self.corpus_map = corpus_map

    def build_context(self, retrieved_chunk_ids: List[UUID]) -> AssembledContext:
        """Constructs an AssembledContext from retrieved chunk IDs."""
        chunks: List[RetrievedContextChunk] = []
        source_map: Dict[str, RetrievedContextChunk] = {}
        formatted_lines = []

        for idx, cid in enumerate(retrieved_chunk_ids, start=1):
            if cid in self.corpus_map:
                chunk = self.corpus_map[cid]
                s_id = f"S{idx}"
                r_chunk = RetrievedContextChunk(
                    source_id=s_id,
                    chunk_id=chunk.chunk_id,
                    document_id=chunk.document_id,
                    document_title=chunk.title,
                    content=chunk.content,
                    token_count=chunk.token_count,
                    page_number=chunk.page_number,
                    section_heading=chunk.section_heading,
                    relevance_score=round(1.0 - (idx * 0.05), 3),
                    rank=idx
                )
                chunks.append(r_chunk)
                source_map[s_id] = r_chunk
                formatted_lines.append(f"[{s_id}] {chunk.title} (Page {chunk.page_number}):\n{chunk.content}")

        formatted_text = "\n\n".join(formatted_lines)
        total_tokens = sum(c.token_count for c in chunks)

        return AssembledContext(
            chunks=chunks,
            formatted_text=formatted_text,
            total_tokens=total_tokens,
            source_map=source_map
        )

    def evaluate_response(
        self,
        case: EvaluationCase,
        response: TutorAnswerResponse,
        context: AssembledContext
    ) -> CaseGenerationEvaluation:
        """Evaluates a single generated TutorAnswerResponse against an EvaluationCase."""
        # Answer Quality
        qual: AnswerQualityResult = evaluate_answer_quality(response.answer, case)

        # Citation and Claim Level Faithfulness
        cite_rep: CitationEvaluationReport = evaluate_citations_and_claims(response.answer, context)

        return CaseGenerationEvaluation(
            case_id=case.case_id,
            subject=case.subject,
            question=case.question,
            generated_answer=response.answer,
            grounding_status=response.grounding_status,
            insufficient_evidence=response.insufficient_evidence,
            correctness_score=qual.correctness_score,
            relevance_score=qual.relevance_score,
            completeness_score=qual.completeness_score,
            citation_precision=cite_rep.citation_precision,
            citation_coverage=cite_rep.citation_coverage,
            claim_faithfulness=cite_rep.claim_faithfulness,
            hallucination_rate=cite_rep.hallucination_rate,
            source_id_validity_rate=cite_rep.source_id_validity_rate,
            matched_facts=qual.matched_facts,
            missing_facts=qual.missing_facts
        )

    def evaluate_batch(
        self,
        eval_pairs: List[Tuple[EvaluationCase, TutorAnswerResponse, AssembledContext]]
    ) -> AggregateGenerationReport:
        """Evaluates a batch of RAG responses and returns aggregate metrics."""
        case_evals: List[CaseGenerationEvaluation] = []
        grounded_count = 0
        partial_count = 0
        insufficient_count = 0

        for case, resp, ctx in eval_pairs:
            ev = self.evaluate_response(case, resp, ctx)
            case_evals.append(ev)

            if ev.grounding_status == GroundingStatus.GROUNDED:
                grounded_count += 1
            elif ev.grounding_status == GroundingStatus.PARTIALLY_GROUNDED:
                partial_count += 1
            else:
                insufficient_count += 1

        n = len(case_evals)
        if n == 0:
            return AggregateGenerationReport(
                sample_count=0,
                mean_correctness=0.0,
                mean_relevance=0.0,
                mean_completeness=0.0,
                mean_citation_precision=0.0,
                mean_citation_coverage=0.0,
                mean_claim_faithfulness=0.0,
                mean_hallucination_rate=0.0,
                mean_source_id_validity=0.0,
                grounded_count=0,
                partially_grounded_count=0,
                insufficient_evidence_count=0,
                case_evaluations=[]
            )

        return AggregateGenerationReport(
            sample_count=n,
            mean_correctness=round(sum(e.correctness_score for e in case_evals) / n, 4),
            mean_relevance=round(sum(e.relevance_score for e in case_evals) / n, 4),
            mean_completeness=round(sum(e.completeness_score for e in case_evals) / n, 4),
            mean_citation_precision=round(sum(e.citation_precision for e in case_evals) / n, 4),
            mean_citation_coverage=round(sum(e.citation_coverage for e in case_evals) / n, 4),
            mean_claim_faithfulness=round(sum(e.claim_faithfulness for e in case_evals) / n, 4),
            mean_hallucination_rate=round(sum(e.hallucination_rate for e in case_evals) / n, 4),
            mean_source_id_validity=round(sum(e.source_id_validity_rate for e in case_evals) / n, 4),
            grounded_count=grounded_count,
            partially_grounded_count=partial_count,
            insufficient_evidence_count=insufficient_count,
            case_evaluations=case_evals
        )

