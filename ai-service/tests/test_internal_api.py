"""
CS Scholar AI - Internal Ingestion API Unit Tests
"""
import uuid
from unittest.mock import MagicMock, patch
import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.core.config import settings
from app.database.connection import get_db

client = TestClient(app)


def test_internal_api_missing_key():
    payload = {
        "document_id": str(uuid.uuid4()),
        "workspace_id": str(uuid.uuid4()),
        "storage_key": "workspaces/1/doc.pdf",
        "file_type": "PDF",
        "processing_version": 1
    }
    response = client.post("/internal/v1/ingestion/process", json=payload)
    # Missing required header should yield 401 or 422
    assert response.status_code in [401, 422]


def test_internal_api_invalid_key():
    payload = {
        "document_id": str(uuid.uuid4()),
        "workspace_id": str(uuid.uuid4()),
        "storage_key": "workspaces/1/doc.pdf",
        "file_type": "PDF",
        "processing_version": 1
    }
    response = client.post(
        "/internal/v1/ingestion/process",
        json=payload,
        headers={"X-Internal-Service-Key": "invalid_wrong_key"}
    )
    assert response.status_code == 403


def test_internal_api_valid_key_process_success():
    doc_id = uuid.uuid4()
    ws_id = uuid.uuid4()
    job_id = uuid.uuid4()

    mock_db = MagicMock()
    mock_doc = MagicMock()
    mock_doc.id = doc_id
    mock_doc.processing_status = "PENDING_UPLOAD"
    mock_db.query.return_value.filter.return_value.first.return_value = mock_doc

    mock_job = MagicMock()
    mock_job.id = job_id
    mock_job.status = "QUEUED"

    app.dependency_overrides[get_db] = lambda: mock_db

    try:
        with patch("app.api.routes.internal.ingestion.ProcessingJob", return_value=mock_job):
            with patch("app.api.routes.internal.ingestion.process_document_task.delay") as mock_delay:
                mock_task = MagicMock()
                mock_task.id = "celery-task-123"
                mock_delay.return_value = mock_task

                payload = {
                    "document_id": str(doc_id),
                    "workspace_id": str(ws_id),
                    "storage_key": "workspaces/ws/documents/doc/file.pdf",
                    "file_type": "PDF",
                    "processing_version": 1
                }
                response = client.post(
                    "/internal/v1/ingestion/process",
                    json=payload,
                    headers={"X-Internal-Service-Key": settings.internal_service_key}
                )

                assert response.status_code == 202
                data = response.json()
                assert data["status"] == "QUEUED"
                assert data["document_id"] == str(doc_id)
                assert data["task_id"] == "celery-task-123"
    finally:
        app.dependency_overrides.clear()

