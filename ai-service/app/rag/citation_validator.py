"""
CS Scholar AI - Citation Validator & Grounding Auditor
Validates model citations against retrieved source IDs, extracts evidence excerpts,
and computes final grounding status.
"""
import logging
import re
from typing import List, Dict, Set, Tuple
from app.rag.schemas import (
    AssembledContext,
    VerifiedCitation,
    GroundingStatus,
    RetrievedContextChunk
)

logger = logging.getLogger(__name__)


class CitationValidator:
    """Audits and validates citations emitted by the LLM against retrieved chunks."""

    @staticmethod
    def extract_inline_citations(text: str) -> Set[str]:
        """Extracts all [S1], [S2] tags found inline in text."""
        matches = re.findall(r"\[(S\d+)\]", text)
        return set(matches)

    def validate(
        self,
        answer: str,
        insufficient_evidence_flag: bool,
        claimed_sources: List[str],
        context: AssembledContext
    ) -> Tuple[List[VerifiedCitation], GroundingStatus, bool]:
        """
        Validates citations and computes GroundingStatus.

        Returns:
            (verified_citations, grounding_status, insufficient_evidence)
        """
        source_map = context.source_map
        inline_sources = self.extract_inline_citations(answer)
        all_claimed = set(claimed_sources).union(inline_sources)

        verified_citations: List[VerifiedCitation] = []
        valid_source_ids: Set[str] = set()
        hallucinated_source_ids: Set[str] = set()

        for s_id in sorted(all_claimed):
            if s_id in source_map:
                valid_source_ids.add(s_id)
                chunk: RetrievedContextChunk = source_map[s_id]

                # Extract a concise representative excerpt
                raw_content = chunk.content.strip()
                excerpt = raw_content[:200] + ("..." if len(raw_content) > 200 else "")

                verified_citations.append(
                    VerifiedCitation(
                        source_id=s_id,
                        chunk_id=chunk.chunk_id,
                        document_id=chunk.document_id,
                        document_title=chunk.document_title,
                        page_number=chunk.page_number,
                        slide_number=chunk.slide_number,
                        section_heading=chunk.section_heading,
                        excerpt=excerpt,
                        relevance_score=chunk.relevance_score,
                        rank=chunk.rank
                    )
                )
            else:
                hallucinated_source_ids.add(s_id)
                logger.warning("Rejected unverified/hallucinated citation ID: %s", s_id)

        # Determine GroundingStatus
        if insufficient_evidence_flag:
            status = GroundingStatus.INSUFFICIENT_EVIDENCE
            final_insufficient = True
        elif len(context.chunks) == 0:
            # Answered from conversational context or conceptual dialogue
            status = GroundingStatus.GROUNDED
            final_insufficient = False
        elif hallucinated_source_ids and valid_source_ids:
            status = GroundingStatus.PARTIALLY_GROUNDED
            final_insufficient = False
        elif not valid_source_ids:
            # Model generated an answer without citations or all were hallucinated
            if not insufficient_evidence_flag and len(answer.strip()) > 15 and "could not find any relevant materials" not in answer.lower():
                # Model provided a conceptual or conversational response (e.g. clarification, Hinglish translation, domain explanation)
                status = GroundingStatus.GROUNDED
                final_insufficient = False
            else:
                status = GroundingStatus.INSUFFICIENT_EVIDENCE
                final_insufficient = True
        else:
            status = GroundingStatus.GROUNDED
            final_insufficient = False

        logger.info(
            "Citation validation: verified=%d, hallucinated=%d, grounding_status=%s",
            len(verified_citations), len(hallucinated_source_ids), status.value
        )

        return verified_citations, status, final_insufficient

