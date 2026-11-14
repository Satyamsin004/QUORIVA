"""
CS Scholar AI - API Integration Tests for Internal Academic Tutor Endpoint
Tests /internal/v1/tutor/answer authentication, payload validation, and responses.
"""
from unittest.mock import patch, MagicMock
from uuid import uuid4
from fastapi.testclient import TestClient

from app.main import app
from app.core.config import settings
from app.rag.schemas import TutorAnswerResponse, GroundingStatus, VerifiedCitation

client = TestClient(app)

INTERNAL_KEY = settings.internal_service_key


def test_tutor_endpoint_missing_auth():
    payload = {
        "query": "explain page replacement",
        "workspace_id": str(uuid4()),
        "user_id": str(uuid4())
    }
    response = client.post("/internal/v1/tutor/answer", json=payload)
    # 401 Unauthorized or 422 Unprocessable Entity
    assert response.status_code in (401, 422)


def test_tutor_endpoint_invalid_auth():
    payload = {
        "query": "explain page replacement",
        "workspace_id": str(uuid4()),
        "user_id": str(uuid4())
    }
    headers = {"X-Internal-Service-Key": "wrong_key"}
    response = client.post("/internal/v1/tutor/answer", json=payload, headers=headers)
    assert response.status_code in (401, 403)


def test_tutor_endpoint_success():
    mock_answer = TutorAnswerResponse(
        answer="FIFO page replacement replaces the oldest page [S1].",
        grounding_status=GroundingStatus.GROUNDED,
        insufficient_evidence=False,
        citations=[
            VerifiedCitation(
                source_id="S1",
                chunk_id=uuid4(),
                document_id=uuid4(),
                document_title="OS Silberschatz.pdf",
                page_number=312,
                slide_number=None,
                section_heading="Page Replacement",
                excerpt="FIFO replaces the oldest page.",
                relevance_score=0.94,
                rank=1
            )
        ],
        provider="groq",
        model_name="llama-3.3-70b-versatile",
        input_tokens=120,
        output_tokens=40,
        latency_ms=180
    )

    payload = {
        "query": "explain FIFO page replacement",
        "workspace_id": str(uuid4()),
        "user_id": str(uuid4()),
        "history": [
            {"role": "user", "content": "What is paging?"},
            {"role": "assistant", "content": "Paging is a memory management scheme."}
        ]
    }
    headers = {"X-Internal-Service-Key": INTERNAL_KEY}

    with patch("app.api.routes.internal.tutor._rag_service.answer", return_value=mock_answer):
        response = client.post("/internal/v1/tutor/answer", json=payload, headers=headers)

    assert response.status_code == 200
    data = response.json()
    assert "FIFO page replacement replaces" in data["answer"]
    assert data["grounding_status"] == "GROUNDED"
    assert data["insufficient_evidence"] is False
    assert len(data["citations"]) == 1
    assert data["citations"][0]["source_id"] == "S1"
    assert data["citations"][0]["page_number"] == 312
    assert data["provider"] == "groq"
