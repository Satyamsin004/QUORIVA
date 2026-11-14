"""
Unit tests for Internal Retrieval Search Endpoint (/internal/v1/retrieval/search)
"""
import uuid
import pytest
from unittest.mock import MagicMock
from fastapi.testclient import TestClient

from app.main import app
from app.core.config import settings
from app.retrieval.schemas import InternalSearchResponse, SearchResultItem

client = TestClient(app)
VALID_KEY = settings.internal_service_key


def test_search_endpoint_missing_auth():
    payload = {
        "query": "binary search tree",
        "workspaceId": str(uuid.uuid4()),
        "userId": str(uuid.uuid4()),
        "topK": 10
    }
    response = client.post("/internal/v1/retrieval/search", json=payload)
    assert response.status_code in (401, 422)


def test_search_endpoint_invalid_auth():
    payload = {
        "query": "binary search tree",
        "workspaceId": str(uuid.uuid4()),
        "userId": str(uuid.uuid4()),
        "topK": 10
    }
    headers = {"X-Internal-Service-Key": "wrong_key"}
    response = client.post("/internal/v1/retrieval/search", json=payload, headers=headers)
    assert response.status_code == 403


def test_search_endpoint_empty_query():
    payload = {
        "query": "   ",
        "workspaceId": str(uuid.uuid4()),
        "userId": str(uuid.uuid4()),
        "topK": 10
    }
    headers = {"X-Internal-Service-Key": VALID_KEY}
    response = client.post("/internal/v1/retrieval/search", json=payload, headers=headers)
    assert response.status_code in (400, 422)


def test_search_endpoint_success_mock(monkeypatch):
    ws_id = uuid.uuid4()
    mock_res = InternalSearchResponse(
        query="binary search tree",
        workspace_id=ws_id,
        total_candidates=1,
        results=[
            SearchResultItem(
                chunk_id=uuid.uuid4(),
                document_id=uuid.uuid4(),
                document_name="Algorithms.pdf",
                workspace_id=ws_id,
                chunk_content="BST property states...",
                page_number=12,
                slide_number=None,
                section_heading="Trees",
                chunk_index=3,
                source_type="PDF",
                final_rank=1,
                reranker_score=0.92,
                rrf_score=0.032,
                dense_rank=1,
                keyword_rank=1,
                dense_similarity=0.88,
                keyword_score=0.45,
                segment_index=0
            )
        ]
    )

    from app.api.routes.internal import retrieval as retrieval_route
    monkeypatch.setattr(retrieval_route._retrieval_service, "search", MagicMock(return_value=mock_res))

    payload = {
        "query": "binary search tree",
        "workspaceId": str(ws_id),
        "userId": str(uuid.uuid4()),
        "topK": 5
    }
    headers = {"X-Internal-Service-Key": VALID_KEY}
    response = client.post("/internal/v1/retrieval/search", json=payload, headers=headers)

    assert response.status_code == 200
    data = response.json()
    assert data["query"] == "binary search tree"
    assert data["workspaceId"] == str(ws_id)
    assert len(data["results"]) == 1
    assert data["results"][0]["documentName"] == "Algorithms.pdf"
    assert data["results"][0]["finalRank"] == 1
