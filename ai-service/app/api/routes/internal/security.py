"""
CS Scholar AI - Internal Service Security Dependency
Provides timing-safe HMAC request signing, deterministic canonicalization,
timestamp drift checks, and distributed Redis-backed atomic replay protection.
Enforces verification order: HMAC signature is verified BEFORE nonce consumption.
"""
import hashlib
import hmac
import logging
import time
from typing import Optional
from fastapi import Header, HTTPException, Request, status
import redis

from app.core.config import settings

logger = logging.getLogger(__name__)

MAX_DRIFT_SECONDS = 300  # 5 minutes tolerance window
REPLAY_NONCE_TTL = 360   # 6 minutes TTL in Redis

_redis_client: Optional[redis.Redis] = None


def get_redis_client() -> redis.Redis:
    global _redis_client
    if _redis_client is None:
        _redis_client = redis.Redis(
            host=settings.redis_host,
            port=settings.redis_port,
            password=settings.redis_password or None,
            socket_timeout=5.0,
            socket_connect_timeout=5.0,
            decode_responses=True,
        )
    return _redis_client


def reset_redis_client_for_testing():
    """Allows test fixtures to mock or reset the Redis client."""
    global _redis_client
    _redis_client = None


def canonicalize_query(raw_query: str) -> str:
    """Deterministically sorts query parameters alphabetically by key-value pair."""
    if not raw_query:
        return ""
    params = [p for p in raw_query.split("&") if p.strip()]
    params.sort()
    return "&".join(params)


async def verify_internal_service_key(
    request: Request,
    x_internal_service_key: Optional[str] = Header(None, alias="X-Internal-Service-Key"),
    x_internal_timestamp: Optional[str] = Header(None, alias="X-Internal-Timestamp"),
    x_internal_nonce: Optional[str] = Header(None, alias="X-Internal-Nonce"),
    x_internal_signature: Optional[str] = Header(None, alias="X-Internal-Signature"),
) -> str:
    """Validate internal service communication using HMAC-SHA256 request signing
    with canonical string verification, timestamp drift check, and Redis atomic nonce replay prevention.
    Strictly verifies signature BEFORE consuming nonces to prevent denial-of-service on valid nonces."""
    now = time.time()

    # 1. If HMAC signature headers are present, enforce cryptographic verification
    if x_internal_signature and x_internal_timestamp:
        if not x_internal_nonce:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Missing required internal nonce header."
            )

        try:
            req_time = float(x_internal_timestamp)
        except (ValueError, TypeError):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid timestamp header format."
            )

        if abs(now - req_time) > MAX_DRIFT_SECONDS:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=f"Request timestamp expired or skewed beyond {MAX_DRIFT_SECONDS} seconds."
            )

        # Compute exact transmitted body SHA-256
        body = await request.body()
        body_sha256 = hashlib.sha256(body).hexdigest()
        path = request.url.path
        canonical_query = canonicalize_query(request.url.query)
        method = request.method.upper()

        # Canonical format: METHOD + "\n" + PATH + "\n" + QUERY + "\n" + TIMESTAMP + "\n" + NONCE + "\n" + BODY_SHA256
        canonical_string = f"{method}\n{path}\n{canonical_query}\n{x_internal_timestamp}\n{x_internal_nonce}\n{body_sha256}"
        expected_sig = hmac.new(
            settings.internal_service_key.encode("utf-8"),
            canonical_string.encode("utf-8"),
            hashlib.sha256
        ).hexdigest()

        # Step A: Signature MUST be verified BEFORE nonce consumption in Redis
        if not hmac.compare_digest(expected_sig, x_internal_signature):
            logger.warning("Invalid HMAC signature received for path: %s. Expected sig: %s vs got: %s. Canonical string was:\n%r", path, expected_sig, x_internal_signature, canonical_string)
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Invalid internal HMAC signature."
            )

        # Step B: Atomic distributed replay protection check via Redis (only on valid signature)
        redis_key = f"cs:replay:nonce:{x_internal_nonce}"
        try:
            r = get_redis_client()
            is_new = r.set(redis_key, "1", ex=REPLAY_NONCE_TTL, nx=True)
            if not is_new:
                logger.warning("Replay attack prevented for nonce: %s", x_internal_nonce[:8] + "...")
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail="Replay attack detected: nonce has already been used."
                )
        except redis.RedisError as e:
            logger.warning("Redis replay store warning during internal request verification: %s. HMAC signature already verified.", e)

        return x_internal_signature

    # 2. Fallback to constant-time static key verification (if HMAC signature is not supplied)
    if not x_internal_service_key:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing required internal service key or signature."
        )

    expected = settings.internal_service_key.encode("utf-8")
    actual = x_internal_service_key.encode("utf-8")

    if not hmac.compare_digest(expected, actual):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Invalid internal service key."
        )

    return x_internal_service_key
