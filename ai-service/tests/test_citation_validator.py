"""
CS Scholar AI - Unit Tests for Citation Validator
Tests citation validation against source_map, handling of hallucinated IDs, and GroundingStatus.
"""
from uuid import uuid4
from app.rag.citation_validator import CitationValidator
from app.rag.schemas import AssembledContext, RetrievedContextChunk, GroundingStatus


def create_mock_context() -> AssembledContext:
    chunk1 = RetrievedContextChunk(
        source_id="S1",
        chunk_id=uuid4(),
        document_id=uuid4(),
        document_title="Algorithms.pdf",
        content="Dijkstra algorithm finds shortest paths in non-negative weighted graphs.",
        token_count=12,
        page_number=102,
        slide_number=None,
        section_heading="Single-Source Shortest Paths",
        relevance_score=0.91,
        rank=1
    )
    chunk2 = RetrievedContextChunk(
        source_id="S2",
        chunk_id=uuid4(),
        document_id=uuid4(),
        document_title="Networks.pdf",
        content="TCP uses congestion window to regulate sender transmission rates.",
        token_count=10,
        page_number=45,
        slide_number=None,
        section_heading="TCP Congestion Control",
        relevance_score=0.88,
        rank=2
    )
    return AssembledContext(
        chunks=[chunk1, chunk2],
        formatted_text="[S1] Algorithms.pdf\n[S2] Networks.pdf",
        total_tokens=22,
        source_map={"S1": chunk1, "S2": chunk2}
    )


def test_citation_validator_fully_grounded():
    validator = CitationValidator()
    context = create_mock_context()

    answer = "Dijkstra algorithm handles non-negative edge weights [S1], whereas TCP controls rates [S2]."
    claimed = ["S1", "S2"]

    verified, status, insufficient = validator.validate(
        answer=answer,
        insufficient_evidence_flag=False,
        claimed_sources=claimed,
        context=context
    )

    assert status == GroundingStatus.GROUNDED
    assert insufficient is False
    assert len(verified) == 2
    assert verified[0].source_id == "S1"
    assert verified[0].document_title == "Algorithms.pdf"
    assert verified[1].source_id == "S2"
    assert verified[1].document_title == "Networks.pdf"


def test_citation_validator_partially_grounded_with_hallucination():
    validator = CitationValidator()
    context = create_mock_context()

    # S99 does not exist in context
    answer = "Dijkstra works on graphs [S1] and Quantum computing solves NP [S99]."
    claimed = ["S1", "S99"]

    verified, status, insufficient = validator.validate(
        answer=answer,
        insufficient_evidence_flag=False,
        claimed_sources=claimed,
        context=context
    )

    assert status == GroundingStatus.PARTIALLY_GROUNDED
    assert insufficient is False
    assert len(verified) == 1
    assert verified[0].source_id == "S1"


def test_citation_validator_insufficient_evidence():
    validator = CitationValidator()
    context = create_mock_context()

    answer = "The provided materials do not discuss Quantum Machine Learning."
    claimed = []

    verified, status, insufficient = validator.validate(
        answer=answer,
        insufficient_evidence_flag=True,
        claimed_sources=claimed,
        context=context
    )

    assert status == GroundingStatus.INSUFFICIENT_EVIDENCE
    assert insufficient is True
    assert len(verified) == 0


def test_citation_validator_inline_regex_fallback():
    validator = CitationValidator()
    context = create_mock_context()

    # Model neglected to populate claimed_sources list, but cited [S1] inline
    answer = "Dijkstra algorithm uses greedy strategy [S1]."
    claimed = []

    verified, status, insufficient = validator.validate(
        answer=answer,
        insufficient_evidence_flag=False,
        claimed_sources=claimed,
        context=context
    )

    assert status == GroundingStatus.GROUNDED
    assert len(verified) == 1
    assert verified[0].source_id == "S1"

