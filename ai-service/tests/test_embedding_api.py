"""
CS Scholar AI - Internal Embedding API Unit Tests
"""
import uuid
from unittest.mock import MagicMock, patch
from fastapi.testclient import TestClient

from app.main import app
from app.core.config import settings
from app.database.connection import get_db

client = TestClient(app)


def test_embedding_api_missing_key():
    payload = {
        "document_id": str(uuid.uuid4()),
        "processing_version": 1
    }
    response = client.post("/internal/v1/embeddings/process", json=payload)
    assert response.status_code in [401, 422]


def test_embedding_api_invalid_key():
    payload = {
        "document_id": str(uuid.uuid4()),
        "processing_version": 1
    }
    response = client.post(
        "/internal/v1/embeddings/process",
        json=payload,
        headers={"X-Internal-Service-Key": "invalid_wrong_key"}
    )
    assert response.status_code == 403


def test_embedding_api_valid_key_success():
    doc_id = uuid.uuid4()
    job_id = uuid.uuid4()

    mock_db = MagicMock()
    mock_doc = MagicMock()
    mock_doc.id = doc_id
    mock_db.query.return_value.filter.return_value.first.return_value = mock_doc

    mock_job = MagicMock()
    mock_job.id = job_id
    mock_job.status = "QUEUED"

    app.dependency_overrides[get_db] = lambda: mock_db

    try:
        with patch("app.api.routes.internal.embedding.EmbeddingJob", return_value=mock_job):
            with patch("app.api.routes.internal.embedding.generate_document_embeddings.delay") as mock_delay:
                mock_task = MagicMock()
                mock_task.id = "celery-embedding-task-456"
                mock_delay.return_value = mock_task

                payload = {
                    "document_id": str(doc_id),
                    "processing_version": 1,
                    "force_reembed": False
                }
                response = client.post(
                    "/internal/v1/embeddings/process",
                    json=payload,
                    headers={"X-Internal-Service-Key": settings.internal_service_key}
                )

                assert response.status_code == 202
                data = response.json()
                assert data["status"] == "QUEUED"
                assert data["document_id"] == str(doc_id)
                assert data["task_id"] == "celery-embedding-task-456"
    finally:
        app.dependency_overrides.clear()

