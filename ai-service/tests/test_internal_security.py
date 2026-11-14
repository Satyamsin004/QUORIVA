import hashlib
import hmac
import time
import uuid
from unittest.mock import MagicMock, patch
import pytest
import redis
from fastapi.testclient import TestClient

from app.main import app
from app.core.config import settings
from app.database.connection import get_db
import app.api.routes.internal.security as sec_module

client = TestClient(app)


def _sign(method: str, path: str, timestamp: str, nonce: str, body: str, query: str = "", key: str = None) -> str:
    key_str = key or settings.internal_service_key
    body_sha256 = hashlib.sha256(body.encode("utf-8")).hexdigest()
    canonical_query = sec_module.canonicalize_query(query)
    canonical = f"{method.upper()}\n{path}\n{canonical_query}\n{timestamp}\n{nonce}\n{body_sha256}"
    return hmac.new(
        key_str.encode("utf-8"),
        canonical.encode("utf-8"),
        hashlib.sha256
    ).hexdigest()


@pytest.fixture
def mock_db_session():
    doc_id = uuid.uuid4()
    job_id = uuid.uuid4()

    mock_db = MagicMock()
    mock_doc = MagicMock()
    mock_doc.id = doc_id
    mock_db.query.return_value.filter.return_value.first.return_value = mock_doc

    mock_job = MagicMock()
    mock_job.id = job_id
    mock_job.status = "QUEUED"
    mock_job.document_id = doc_id

    app.dependency_overrides[get_db] = lambda: mock_db
    with patch("app.api.routes.internal.embedding.EmbeddingJob", return_value=mock_job):
        with patch("app.api.routes.internal.embedding.generate_document_embeddings.delay") as mock_delay:
            mock_task = MagicMock()
            mock_task.id = "task-sec-123"
            mock_delay.return_value = mock_task
            yield doc_id
    app.dependency_overrides.clear()


def test_hmac_valid_signature(mock_db_session):
    path = "/internal/v1/embeddings/process"
    doc_id = mock_db_session
    body = f'{{"document_id": "{doc_id}", "processing_version": 1, "force_reembed": false}}'
    ts = str(int(time.time()))
    nonce = str(uuid.uuid4())
    sig = _sign("POST", path, ts, nonce, body)

    headers = {
        "X-Internal-Timestamp": ts,
        "X-Internal-Nonce": nonce,
        "X-Internal-Signature": sig,
        "Content-Type": "application/json",
    }
    response = client.post(path, content=body, headers=headers)
    assert response.status_code == 202
    data = response.json()
    assert data["status"] == "QUEUED"


def test_hmac_missing_nonce(mock_db_session):
    path = "/internal/v1/embeddings/process"
    doc_id = mock_db_session
    body = f'{{"document_id": "{doc_id}", "processing_version": 1, "force_reembed": false}}'
    ts = str(int(time.time()))
    sig = _sign("POST", path, ts, "dummy", body)

    headers = {
        "X-Internal-Timestamp": ts,
        "X-Internal-Signature": sig,
        "Content-Type": "application/json",
    }
    response = client.post(path, content=body, headers=headers)
    assert response.status_code == 401
    assert "Missing required internal nonce header" in response.json()["detail"]


def test_hmac_expired_timestamp(mock_db_session):
    path = "/internal/v1/embeddings/process"
    doc_id = mock_db_session
    body = f'{{"document_id": "{doc_id}", "processing_version": 1, "force_reembed": false}}'
    ts = str(int(time.time()) - 400)  # 400s in the past (> 300s limit)
    nonce = str(uuid.uuid4())
    sig = _sign("POST", path, ts, nonce, body)

    headers = {
        "X-Internal-Timestamp": ts,
        "X-Internal-Nonce": nonce,
        "X-Internal-Signature": sig,
        "Content-Type": "application/json",
    }
    response = client.post(path, content=body, headers=headers)
    assert response.status_code == 401
    assert "expired or skewed" in response.json()["detail"]


def test_hmac_tampered_body(mock_db_session):
    path = "/internal/v1/embeddings/process"
    doc_id = mock_db_session
    original_body = f'{{"document_id": "{doc_id}", "processing_version": 1, "force_reembed": false}}'
    ts = str(int(time.time()))
    nonce = str(uuid.uuid4())
    sig = _sign("POST", path, ts, nonce, original_body)

    tampered_body = f'{{"document_id": "{uuid.uuid4()}", "processing_version": 1, "force_reembed": false}}'
    headers = {
        "X-Internal-Timestamp": ts,
        "X-Internal-Nonce": nonce,
        "X-Internal-Signature": sig,
        "Content-Type": "application/json",
    }
    response = client.post(path, content=tampered_body, headers=headers)
    assert response.status_code == 403
    assert "Invalid internal HMAC signature" in response.json()["detail"]


def test_hmac_tampered_nonce(mock_db_session):
    path = "/internal/v1/embeddings/process"
    doc_id = mock_db_session
    body = f'{{"document_id": "{doc_id}", "processing_version": 1, "force_reembed": false}}'
    ts = str(int(time.time()))
    nonce = str(uuid.uuid4())
    sig = _sign("POST", path, ts, nonce, body)

    # Tamper with the nonce header
    tampered_nonce = str(uuid.uuid4())
    headers = {
        "X-Internal-Timestamp": ts,
        "X-Internal-Nonce": tampered_nonce,
        "X-Internal-Signature": sig,
        "Content-Type": "application/json",
    }
    response = client.post(path, content=body, headers=headers)
    assert response.status_code == 403
    assert "Invalid internal HMAC signature" in response.json()["detail"]


def test_hmac_query_parameter_canonicalization(mock_db_session):
    # Query parameters in reverse alphabetical order in URL
    raw_query = "version=2&mode=strict&batch=10"
    path = f"/internal/v1/embeddings/process?{raw_query}"
    doc_id = mock_db_session
    body = f'{{"document_id": "{doc_id}", "processing_version": 1, "force_reembed": false}}'
    ts = str(int(time.time()))
    nonce = str(uuid.uuid4())

    # Sign using canonicalized query string
    sig = _sign("POST", "/internal/v1/embeddings/process", ts, nonce, body, query=raw_query)

    headers = {
        "X-Internal-Timestamp": ts,
        "X-Internal-Nonce": nonce,
        "X-Internal-Signature": sig,
        "Content-Type": "application/json",
    }
    response = client.post(path, content=body, headers=headers)
    assert response.status_code == 202

    # Tampering query parameter value must be rejected
    tampered_path = "/internal/v1/embeddings/process?version=3&mode=strict&batch=10"
    tampered_nonce = str(uuid.uuid4())
    headers["X-Internal-Nonce"] = tampered_nonce
    response2 = client.post(tampered_path, content=body, headers=headers)
    assert response2.status_code == 403
    assert "Invalid internal HMAC signature" in response2.json()["detail"]


def test_invalid_signature_does_not_consume_nonce(mock_db_session):
    """Verifies that signature verification occurs BEFORE nonce consumption,
    ensuring invalid signatures cannot burn a valid nonce."""
    path = "/internal/v1/embeddings/process"
    doc_id = mock_db_session
    body = f'{{"document_id": "{doc_id}", "processing_version": 1, "force_reembed": false}}'
    ts = str(int(time.time()))
    nonce = str(uuid.uuid4())
    valid_sig = _sign("POST", path, ts, nonce, body)

    # 1. Attacker sends the victim's nonce with an INVALID signature
    attacker_headers = {
        "X-Internal-Timestamp": ts,
        "X-Internal-Nonce": nonce,
        "X-Internal-Signature": "invalid_forged_signature_0000000000000000000000000000000000000000",
        "Content-Type": "application/json",
    }
    attacker_response = client.post(path, content=body, headers=attacker_headers)
    assert attacker_response.status_code == 403
    assert "Invalid internal HMAC signature" in attacker_response.json()["detail"]

    # 2. Honest request arrives with the SAME nonce and the VALID signature: MUST SUCCEED
    honest_headers = {
        "X-Internal-Timestamp": ts,
        "X-Internal-Nonce": nonce,
        "X-Internal-Signature": valid_sig,
        "Content-Type": "application/json",
    }
    honest_response = client.post(path, content=body, headers=honest_headers)
    assert honest_response.status_code == 202
    assert honest_response.json()["status"] == "QUEUED"

    # 3. Subsequent replay of the now-used nonce MUST fail
    replay_response = client.post(path, content=body, headers=honest_headers)
    assert replay_response.status_code == 403
    assert "Replay attack detected" in replay_response.json()["detail"]


def test_hmac_replay_prevention(mock_db_session):
    path = "/internal/v1/embeddings/process"
    doc_id = mock_db_session
    body = f'{{"document_id": "{doc_id}", "processing_version": 1, "force_reembed": false}}'
    ts = str(int(time.time()))
    nonce = str(uuid.uuid4())
    sig = _sign("POST", path, ts, nonce, body)

    headers = {
        "X-Internal-Timestamp": ts,
        "X-Internal-Nonce": nonce,
        "X-Internal-Signature": sig,
        "Content-Type": "application/json",
    }
    # First request succeeds and records nonce in Redis
    r1 = client.post(path, content=body, headers=headers)
    assert r1.status_code == 202

    # Second request with the same nonce is blocked as replay
    r2 = client.post(path, content=body, headers=headers)
    assert r2.status_code == 403
    assert "Replay attack detected" in r2.json()["detail"]


def test_hmac_redis_failure_fails_closed(mock_db_session):
    path = "/internal/v1/embeddings/process"
    doc_id = mock_db_session
    body = f'{{"document_id": "{doc_id}", "processing_version": 1, "force_reembed": false}}'
    ts = str(int(time.time()))
    nonce = str(uuid.uuid4())
    sig = _sign("POST", path, ts, nonce, body)

    headers = {
        "X-Internal-Timestamp": ts,
        "X-Internal-Nonce": nonce,
        "X-Internal-Signature": sig,
        "Content-Type": "application/json",
    }

    mock_redis = MagicMock()
    mock_redis.set.side_effect = redis.ConnectionError("Redis connection refused")

    with patch.object(sec_module, "get_redis_client", return_value=mock_redis):
        response = client.post(path, content=body, headers=headers)
        assert response.status_code == 503
        assert "Replay protection store unavailable" in response.json()["detail"]


def test_static_key_backward_compatible(mock_db_session):
    path = "/internal/v1/embeddings/process"
    doc_id = mock_db_session
    body = f'{{"document_id": "{doc_id}", "processing_version": 1, "force_reembed": false}}'
    headers = {
        "X-Internal-Service-Key": settings.internal_service_key,
        "Content-Type": "application/json",
    }
    response = client.post(path, content=body, headers=headers)
    assert response.status_code == 202


def test_missing_auth_headers():
    path = "/internal/v1/embeddings/process"
    body = '{"document_id": "00000000-0000-0000-0000-000000000000", "processing_version": 1}'
    response = client.post(path, content=body)
    assert response.status_code == 401
