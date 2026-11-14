"""
CS Scholar AI - Unit Tests for RAG Service
Tests RAG orchestration, empty results handling, and grounding status outputs.
"""
from unittest.mock import MagicMock
from uuid import uuid4
from app.rag.service import RAGService
from app.rag.schemas import InternalTutorChatRequest, GroundingStatus
from app.retrieval.schemas import InternalSearchResponse, SearchResultItem
from app.providers.schemas import LLMResponse


def test_rag_service_empty_search_results():
    mock_retrieval = MagicMock()
    mock_retrieval.search.return_value = InternalSearchResponse(
        query="what is a red-black tree",
        workspace_id=uuid4(),
        total_candidates=0,
        results=[]
    )

    rag = RAGService(retrieval_service=mock_retrieval)
    req = InternalTutorChatRequest(
        query="what is a red-black tree",
        workspace_id=uuid4(),
        user_id=uuid4()
    )

    res = rag.answer(db=MagicMock(), request=req)
    assert res.grounding_status == GroundingStatus.INSUFFICIENT_EVIDENCE
    assert res.insufficient_evidence is True
    assert "could not find any relevant materials" in res.answer
    assert len(res.citations) == 0


def test_rag_service_successful_generation():
    doc_id = uuid4()
    chunk_id = uuid4()
    mock_retrieval = MagicMock()
    mock_retrieval.search.return_value = InternalSearchResponse(
        query="explain raft consensus",
        workspace_id=uuid4(),
        total_candidates=1,
        results=[
            SearchResultItem(
                chunk_id=chunk_id,
                document_id=doc_id,
                document_name="Distributed Systems.pdf",
                workspace_id=uuid4(),
                chunk_content="Raft decomposes consensus into leader election, log replication, and safety.",
                page_number=18,
                slide_number=None,
                section_heading="Raft Overview",
                chunk_index=0,
                source_type="pdf",
                final_rank=1,
                reranker_score=0.96,
                rrf_score=0.03,
                dense_rank=1,
                keyword_rank=1,
                dense_similarity=0.89,
                keyword_score=1.5,
                segment_index=0
            )
        ]
    )

    mock_provider = MagicMock()
    mock_provider.generate.return_value = LLMResponse(
        content="raw content",
        parsed_json={
            "answer": "Raft achieves consensus via leader election and log replication [S1].",
            "insufficient_evidence": False,
            "cited_sources": ["S1"]
        },
        provider="groq",
        model_name="llama-3.3-70b-versatile",
        input_tokens=150,
        output_tokens=30,
        latency_ms=250
    )

    rag = RAGService(retrieval_service=mock_retrieval)
    req = InternalTutorChatRequest(
        query="explain raft consensus",
        workspace_id=uuid4(),
        user_id=uuid4()
    )

    with MagicMock() as mock_db:
        # Patch LLMProviderFactory
        from unittest.mock import patch
        with patch("app.rag.service.LLMProviderFactory.get_provider", return_value=mock_provider):
            res = rag.answer(db=mock_db, request=req)

    assert res.grounding_status == GroundingStatus.GROUNDED
    assert res.insufficient_evidence is False
    assert len(res.citations) == 1
    assert res.citations[0].source_id == "S1"
    assert res.citations[0].document_title == "Distributed Systems.pdf"
    assert res.citations[0].page_number == 18
    assert res.provider == "groq"
    assert res.latency_ms == 250

