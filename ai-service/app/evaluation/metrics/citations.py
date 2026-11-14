"""
CS Scholar AI - Phase 12 Citation & Grounding Metrics
Implements claim-level citation verification, clearly distinguishing:
  1. Source-ID Validity (syntactic reference exists in context)
  2. Citation Correctness (cited source actually supports the specific claim)
  3. Claim-Level Faithfulness (fraction of atomic claims supported by cited sources)
"""
import re
from typing import List, Dict, Set, Tuple, Any
from dataclasses import dataclass
from app.rag.schemas import AssembledContext, RetrievedContextChunk


@dataclass
class ClaimVerificationResult:
    claim_text: str
    cited_source_ids: List[str]
    valid_source_ids: List[str]
    hallucinated_source_ids: List[str]
    supported_by_sources: List[str]
    is_faithful: bool


@dataclass
class CitationEvaluationReport:
    total_claims: int
    supported_claims: int
    total_citations_emitted: int
    valid_source_id_count: int
    correct_citation_count: int
    source_id_validity_rate: float
    citation_precision: float
    citation_coverage: float
    claim_faithfulness: float
    hallucination_rate: float
    claim_details: List[ClaimVerificationResult]


def extract_claims(text: str) -> List[str]:
    """
    Split generated response into atomic sentences/claims, stripping citation tags for evaluation.
    """
    # Clean citations temporarily to find clean sentence boundaries
    sentences = re.split(r'(?<=[.!?])\s+', text.strip())
    claims = []
    for s in sentences:
        s_clean = s.strip()
        if len(s_clean) > 10 and not s_clean.startswith("#"):
            claims.append(s_clean)
    return claims


def extract_citation_tags(text: str) -> List[str]:
    """Extract [S1], [S2] tags from a sentence or passage."""
    return re.findall(r'\[(S\d+)\]', text)


def evaluate_citations_and_claims(
    answer: str,
    context: AssembledContext
) -> CitationEvaluationReport:
    """
    Evaluates answer claims against the retrieved context.

    Distinguishes:
    - Source-ID validity: Does [S1] exist in context.source_map?
    - Citation correctness: Does S1's content actually support the claim text?
    - Claim faithfulness: Is the claim substantiated by the cited evidence?
    """
    source_map = context.source_map
    claims = extract_claims(answer)
    total_claims = len(claims)

    if total_claims == 0:
        return CitationEvaluationReport(
            total_claims=0,
            supported_claims=0,
            total_citations_emitted=0,
            valid_source_id_count=0,
            correct_citation_count=0,
            source_id_validity_rate=1.0,
            citation_precision=1.0,
            citation_coverage=1.0,
            claim_faithfulness=1.0,
            hallucination_rate=0.0,
            claim_details=[]
        )

    claim_results: List[ClaimVerificationResult] = []
    total_citations = 0
    valid_sources_count = 0
    correct_citations_count = 0
    supported_claims_count = 0
    claims_with_citations = 0

    for claim in claims:
        cited_sources = extract_citation_tags(claim)
        total_citations += len(cited_sources)
        if cited_sources:
            claims_with_citations += 1

        valid_sources = []
        hallucinated_sources = []
        supported_sources = []

        # Remove citation brackets from claim for lexical overlap comparison
        clean_claim_text = re.sub(r'\[S\d+\]', '', claim).strip().lower()
        claim_keywords = {w for w in re.findall(r'\b[a-zA-Z]{4,}\b', clean_claim_text)}

        # Detect meta-statements / abstention assertions (e.g. "insufficient evidence", "cannot be answered")
        is_abstention_claim = any(phrase in clean_claim_text for phrase in [
            "insufficient evidence",
            "cannot be answered",
            "not contain information",
            "not provided in the",
            "no information regarding",
            "based on the provided documents",
            "provided documents do not contain",
            "do not contain information"
        ])

        for s_id in cited_sources:
            if s_id in source_map:
                valid_sources.append(s_id)
                valid_sources_count += 1
                chunk: RetrievedContextChunk = source_map[s_id]
                chunk_lower = chunk.content.lower()

                # Check evidence support: key concepts must be reflected in the cited chunk
                if claim_keywords:
                    overlap = sum(1 for kw in claim_keywords if kw in chunk_lower)
                    overlap_ratio = overlap / len(claim_keywords)
                    # If at least 35% of informative keywords appear in the cited chunk, consider supported
                    if overlap_ratio >= 0.35:
                        supported_sources.append(s_id)
                        correct_citations_count += 1
                else:
                    supported_sources.append(s_id)
                    correct_citations_count += 1
            else:
                hallucinated_sources.append(s_id)

        if is_abstention_claim and len(cited_sources) == 0:
            is_faithful = True
            supported_claims_count += 1
            claims_with_citations += 1
        else:
            is_faithful = len(supported_sources) > 0 or (len(cited_sources) == 0 and not claim_keywords)
            if is_faithful and len(supported_sources) > 0:
                supported_claims_count += 1

        claim_results.append(
            ClaimVerificationResult(
                claim_text=claim,
                cited_source_ids=cited_sources,
                valid_source_ids=valid_sources,
                hallucinated_source_ids=hallucinated_sources,
                supported_by_sources=supported_sources,
                is_faithful=is_faithful
            )
        )

    source_id_validity_rate = (
        float(valid_sources_count / total_citations) if total_citations > 0 else 1.0
    )
    citation_precision = (
        float(correct_citations_count / total_citations) if total_citations > 0 else 1.0
    )
    citation_coverage = (
        float(claims_with_citations / total_claims) if total_claims > 0 else 0.0
    )
    claim_faithfulness = (
        float(supported_claims_count / total_claims) if total_claims > 0 else 0.0
    )
    hallucination_rate = max(0.0, 1.0 - claim_faithfulness)

    return CitationEvaluationReport(
        total_claims=total_claims,
        supported_claims=supported_claims_count,
        total_citations_emitted=total_citations,
        valid_source_id_count=valid_sources_count,
        correct_citation_count=correct_citations_count,
        source_id_validity_rate=round(source_id_validity_rate, 4),
        citation_precision=round(citation_precision, 4),
        citation_coverage=round(citation_coverage, 4),
        claim_faithfulness=round(claim_faithfulness, 4),
        hallucination_rate=round(hallucination_rate, 4),
        claim_details=claim_results
    )

