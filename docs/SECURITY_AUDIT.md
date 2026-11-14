# CS Scholar AI — Comprehensive Security Audit & Remediation Report

## 1. Executive Summary

This Security Audit Report details the security evaluation, threat modeling, vulnerability discovery, and production hardening remediations implemented for the CS Scholar AI platform across all foundational modules (Phases 0 through 11 and Phase 11.1).

CS Scholar AI processes sensitive academic courseware, student study notes, private workspaces, AI tutor dialogues, and shared community curriculum assets. Protecting user confidentiality, tenant isolation, computational resources, and internal service boundaries is paramount.

During Phase 11 and Phase 11.1, the entire codebase—spanning the Spring Boot backend, Python AI engine, PostgreSQL/pgvector datastores, MinIO/S3 object storage, Redis cluster, and React frontend—underwent a rigorous security audit. Identified vulnerabilities were systematically remediated and verified with automated regression test suites.

---

## 2. Threat Model & Asset Classification

### 2.1 Critical System Assets
1. **Student Academic Documents**: Private PDFs, lecture notes, exam reviews stored in object storage.
2. **Workspace Privacy Boundaries**: Multi-tenant workspace data, user memberships, and document access boundaries.
3. **Authentication Credentials & Session Tokens**: User passwords, JWT access tokens, and refresh token families.
4. **AI Retrieval & Inference Infrastructure**: Internal embeddings, vector indices, cross-encoder rerankers, and LLM orchestration.
5. **Shared Academic Library & Moderation Integrity**: Rights declarations, author attestations, and public discovery catalogue.

### 2.2 Threat Actor Profiles
- **Unauthenticated External Attacker**: Attempts credential stuffing, brute force logins, actuator probing, denial-of-service on AI generation endpoints, IP spoofing, or payload tampering.
- **Malicious Authenticated User (Tenant)**: Attempts Insecure Direct Object Reference (IDOR) attacks to access or modify other users' workspaces, documents, or private flashcards/quizzes.
- **Compromised Client / Stolen Refresh Token**: An attacker possessing a leaked or intercepted refresh token attempting persistent session hijacking across user devices.
- **Malicious Document Uploader**: An adversary uploading weaponized PDF documents containing executable actions, macro-bearing archives, or crafted files designed to crash parser pipelines.

---

## 3. Attack Surface Analysis

| Surface | Protocol / Tech | Trust Boundary | Primary Risks | Remediated Status |
| :--- | :--- | :--- | :--- | :--- |
| Public API Endpoints | HTTPS / REST | Untrusted Internet $\to$ Spring Boot | Credential stuffing, brute force, rate limit bypass | Hardened (Redis Rate Limiting + Proxy Trust) |
| Internal AI Bridge | HTTP / Internal VPC | Spring Boot $\to$ Python FastAPI | Request tampering, service spoofing, replay attacks | Hardened (Canonical HMAC-SHA256 + Redis Nonce) |
| Storage & File Ingestion | S3 API / REST | Client $\to$ MinIO / Spring Boot | Path traversal, zip bombs, malicious scripts, mime spoofing | Hardened (Fail-Closed Scanning + Lifecycle Status) |
| Actuator Diagnostics | HTTP / Actuator | Management $\to$ Spring Boot | Infrastructure discovery, memory leaks, metrics exposure | Hardened (Role-restricted /actuator) |
| Browser Client State | HTTPS / React | Browser $\to$ Spring Boot | XSS token theft, clickjacking, CSRF | Hardened (HttpOnly Cookies + Double-Submit CSRF) |

---

## 4. Security Findings, Remediations & Verification Matrix

### Finding SEC-01: Refresh Token Family Semantics & Reuse Detection (RFC 6819)
- **Severity**: HIGH
- **OWASP Category**: A07:2021 — Identification and Authentication Failures
- **Description**: Previous token rotation replaced the old refresh token with a new one upon refresh, but presenting an already-revoked refresh token either failed silently or revoked all tokens globally across all the user's devices. Additionally, concurrent requests could trigger race conditions during rotation.
- **Remediation**:
  - Added `family_id UUID NOT NULL` to `refresh_tokens` database table (Flyway migration `V14`).
  - Added `@Lock(LockModeType.PESSIMISTIC_WRITE) findByTokenHashForUpdate` in `RefreshTokenRepository.java` to serialize concurrent refresh operations.
  - Implemented family-scoped invalidation (`revokeAllByFamilyId`): presenting an already-revoked token revokes only the compromised device family, terminating attacker persistence while preserving active sessions on the user's other devices.
- **Verification**: Verified by `SecurityHardeningIntegrationTest.shouldRevokeCompromisedFamilyWhilePreservingOtherSessions` and `AuthServiceTest.shouldThrowWhenRefreshTokenIsRevoked`.

### Finding SEC-02: Refresh Token Confidentiality, Exclusive Cookie Delivery, and CSRF Protection
- **Severity**: HIGH
- **OWASP Category**: A07:2021 — Identification and Authentication Failures / A01:2021 — Broken Access Control
- **Description**: Refresh tokens were exposed in JSON response payloads, leading frontend applications to store them in `localStorage`, which is vulnerable to JavaScript exfiltration during cross-site scripting (XSS).
- **Remediation**:
  - Removed `refreshToken` completely from `AuthResponse.java` and public JSON responses.
  - Refresh tokens are delivered **exclusively** via `HttpOnly`, `SameSite=Lax`, `Path=/api/v1/auth` cookies.
  - Removed all `localStorage` and `sessionStorage` references to refresh tokens in frontend (`apiClient.ts`, `AuthContext.tsx`, `authService.ts`).
  - Implemented Double-Submit Cookie CSRF protection: upon login/registration/refresh, a random `XSRF-TOKEN` cookie is set (`Path=/`, `HttpOnly=false`). Mutative auth requests (`/refresh`, `/logout`) authenticate cookies only when the `X-XSRF-TOKEN` request header matches the cookie value.
  - Logout clears both `refresh_token` and `XSRF-TOKEN` cookies (`Max-Age=0`).
- **Verification**: Verified by `AuthControllerIntegrationTest.shouldRequireCsrfForCookieRefresh`, `shouldRevokeRefreshTokenAndClearCookiesOnLogout`, and frontend build/test suites.

### Finding SEC-03: Distributed HMAC Replay Protection with Nonce Tracking and Redis
- **Severity**: HIGH
- **OWASP Category**: A01:2021 — Broken Access Control / Insecure Communication
- **Description**: Internal service requests between Spring Boot and Python AI microservices relied on a static key or an in-memory signature cache that did not scale across multiple worker processes and omitted request nonces and query parameters from canonical signing.
- **Remediation**:
  - Standardized canonical signing format in `InternalRequestSigner.java`:
    `METHOD + "\n" + PATH + "\n" + QUERY + "\n" + TIMESTAMP + "\n" + NONCE + "\n" + BODY_SHA256`
  - Spring Boot generates unique UUID nonces and attaches `X-Internal-Nonce`, `X-Internal-Timestamp`, `X-Internal-Signature`, and `X-Correlation-ID`.
  - In `ai-service/app/api/routes/internal/security.py`:
    - Enforced strict 300-second timestamp drift window.
    - Implemented distributed replay prevention in Redis using atomic `SET cs:replay:nonce:<nonce> 1 EX 360 NX`.
    - Fail-closed behavior: returns HTTP 503 Service Unavailable if Redis is unreachable.
    - Constant-time signature comparison with `hmac.compare_digest`.
    - Sanitized logging without exposing secrets or raw auth tokens.
- **Verification**: Verified by `test_internal_security.py` in `ai-service` (8 automated tests covering valid signatures, missing nonces, timestamp skew, body tampering, duplicate nonce rejection via Redis, and fail-closed Redis error handling).

### Finding SEC-04: Distributed Rate Limiting & Proxy IP Validation
- **Severity**: HIGH
- **OWASP Category**: A04:2021 — Insecure Design / Rate Limiting
- **Description**: Authentication endpoints and resource-intensive AI generation endpoints relied on in-memory counters per container and trusted `X-Forwarded-For` unconditionally, enabling attackers to spoof client IPs and bypass rate limits.
- **Remediation**:
  - Implemented Redis-backed atomic rate limiting in `RateLimitingFilter.java` using `cs:ratelimit:<key>:<minute>` with `INCR` + `EXPIRE`.
  - Enforced proxy validation: `X-Forwarded-For` is only parsed when `request.getRemoteAddr()` matches configured `security.rate-limiting.trusted-proxies`. Untrusted addresses fallback to direct socket IP, preventing IP spoofing.
  - Implemented user-level rate limiting for authenticated AI generation (`/api/v1/learning/**/generate`, limit 15 req/min) and IP-level limits for authentication (`/login`, `/register`, limit 10 req/min).
  - Consistent HTTP 429 response with `Retry-After: 60` header.
  - In-memory sliding-window fallback with warning logs if Redis is temporarily unreachable.
- **Verification**: Verified by `SecurityHardeningIntegrationTest.shouldPreventIpSpoofingFromUntrustedProxies` and `shouldEnforceRateLimitingThreshold`.

### Finding SEC-05: Malware Screening Hardening, Lifecycle Status & Fail-Closed Policy
- **Severity**: MEDIUM
- **OWASP Category**: A08:2021 — Software and Data Integrity Failures
- **Description**: File screening previously failed open on unexpected exceptions and did not persist scanning outcomes in the database, risking ingestion of corrupted or suspicious documents.
- **Remediation**:
  - Classified built-in scanner as heuristic screening (`HeuristicMalwareScanner.java`).
  - Added `scan_status VARCHAR(30) NOT NULL DEFAULT 'CLEAN'` to `documents` table and `Document.java` entity, with check constraint covering `PENDING`, `CLEAN`, `SUSPICIOUS`, `INFECTED`, `SCAN_FAILED`.
  - `DocumentService.requestUploadUrl` initializes document with `ScanStatus.PENDING`.
  - `DocumentService.completeUpload` inspects file stream up to 4MB preview:
    - Rejects test signatures (`INFECTED`), dangerous PDF actions `/Launch`, `/JavaScript` (`SUSPICIOUS`), and macros in OpenXML (`SUSPICIOUS`).
    - Operates strictly fail-closed: any stream timeout or parsing error yields `SCAN_FAILED`.
    - If scan result is not clean (`!scanResult.isAcceptable()`), the uploaded object is deleted from S3 storage, document processing status is set to `FAILED`, and upload completion is aborted with HTTP 400 Bad Request.
    - Only documents with `ScanStatus.CLEAN` can proceed to ingestion.
  - Created production-ready ClamAV daemon TCP adapter (`ClamAvMalwareScanner.java`) implementing `MalwareScanner`.
- **Verification**: Verified by `SecurityHardeningIntegrationTest.shouldDetectMaliciousSignaturesAndFailClosed` and database schema validation.

### Finding SEC-06: Workspace Member Privilege Escalation (IDOR Prevention)
- **Severity**: MEDIUM
- **OWASP Category**: A01:2021 — Broken Access Control
- **Description**: Read-only `VIEWER` members in multi-tenant workspaces could rename workspaces or modify metadata.
- **Remediation**: Added `validateWorkspaceEditorOrOwner` in `WorkspaceService.java`. Non-owner members must hold `OWNER` or `EDITOR` role; `VIEWER` members are explicitly rejected with `ForbiddenException`.
- **Verification**: Verified by `SecurityHardeningIntegrationTest.shouldForbidViewerFromModifyingWorkspace`.

### Finding SEC-07: Actuator Information Disclosure Prevention
- **Severity**: HIGH
- **OWASP Category**: A05:2021 — Security Misconfiguration
- **Description**: Spring Boot Actuator endpoints exposed detailed internal infrastructure information publicly.
- **Remediation**: Configured `management.endpoint.health.show-details: when_authorized` and restricted sensitive endpoints to `ADMIN` role in `SecurityConfig.java`. Only `/actuator/health` and `/actuator/info` are public.
- **Verification**: Verified by Spring Security filter chain configuration in `SecurityConfig.java`.

### Finding SEC-08: Request Correlation & Distributed Tracing
- **Severity**: MEDIUM
- **OWASP Category**: A09:2021 — Security Logging and Monitoring Failures
- **Description**: Requests lacked persistent correlation identifiers across polyglot service boundaries.
- **Remediation**: Implemented `CorrelationIdFilter.java` using SLF4J MDC (`correlationId`), propagating `X-Correlation-ID` across HTTP responses and through `InternalRequestSigner.java` to Python AI services.
- **Verification**: Verified by `SecurityHardeningIntegrationTest.shouldHandleCorrelationIdHeader`.

### Finding SEC-09: HTTP Security Response Headers & Clickjacking Prevention
- **Severity**: MEDIUM
- **OWASP Category**: A05:2021 — Security Misconfiguration
- **Description**: Missing security headers allowed clickjacking and MIME-type sniffing.
- **Remediation**: Configured `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, and `Strict-Transport-Security` in `SecurityConfig.java`.
- **Verification**: Verified via Spring Security filter chain.

### Finding SEC-10: Complete-File Malware Scanning & Composite Pipeline (Phase 11.1.1)
- **Severity**: HIGH
- **OWASP Category**: A08:2021 — Software and Data Integrity Failures
- **Description**: Previously, file scanning inspected only the first 4 MB, leaving trailing portions of large files uninspected, and ClamAV was not wired into the production upload execution path.
- **Remediation**:
  - Implemented `CompositeMalwareScanner.java` (`@Primary`) that chains `ClamAvMalwareScanner` and `HeuristicMalwareScanner`.
  - Spools stream to a bounded temporary file (`Files.createTempFile`) with 64 KB buffers, allowing multi-engine verification of complete files (from byte 0 to EOF) without holding large files in heap memory.
  - Upgraded `HeuristicMalwareScanner.java` with a 64 KB sliding window (256-byte overlap) across entire file streams and exhaustive OpenXML `ZipInputStream` archive inspection (up to 10,000 entries and 100 MB decompression limit).
  - Enforced `security.antivirus.clamav.required=true` policy: fails closed (`SCAN_FAILED`) if ClamAV daemon is unreachable or timed out. In local development (`enabled=false`), explicit development warning is logged.
  - Documents with non-clean status are blocked from download (`generateDownloadUrl`) and ingestion, and threat detections trigger immediate S3 object deletion and persistent `ProcessingStatus.FAILED`.
- **Verification**: Verified by `SecurityHardeningIntegrationTest.shouldScanEntireDocumentPastFirst4Mb` and `shouldDetectMaliciousSignaturesAndFailClosed`.

### Finding SEC-11: Rate-Limiting Outage Policy & Endpoint Risk Classification (Phase 11.1.1)
- **Severity**: HIGH
- **OWASP Category**: A04:2021 — Insecure Design / Rate Limiting
- **Description**: A Redis outage could either cause catastrophic unmetered access to high-cost AI inference endpoints or cause total platform denial of service.
- **Remediation**:
  - Explicitly classified endpoints in `RateLimitingFilter.java`:
    - `HIGH_COST`: AI generation (`/api/v1/learning/**/generate`), tutor responses (`/api/v1/conversations/**/messages`), and deep search (`/api/v1/search/**`).
    - `STANDARD`: Authentication, CRUD, and workspace management.
  - If Redis fails:
    - `HIGH_COST` endpoints strictly **fail closed** returning HTTP 503 (`Retry-After: 60`) with message `"Rate limiting service temporarily unavailable"`.
    - `STANDARD` endpoints degrade to local in-memory sliding-window counters with logged warnings.
  - Exposes operational metrics: `getRedisFailureCount()`, `isRedisAvailable()`, and strict proxy IP validation.
- **Verification**: Verified by `SecurityHardeningIntegrationTest.shouldFailClosedOnRedisOutageForHighCostEndpoints` and `shouldDegradeToLocalFallbackForStandardEndpointsOnRedisOutage`.

### Finding SEC-12: Cryptographically Signed Session-Bound CSRF Token Protection (Phase 11.1.1)
- **Severity**: HIGH
- **OWASP Category**: A01:2021 — Broken Access Control / CSRF
- **Description**: Unsigned random CSRF tokens in double-submit cookies are vulnerable to subdomain cookie injection and cookie tossing attacks.
- **Remediation**:
  - Implemented `CsrfTokenService.java` using HMAC-SHA256:
    $$\text{Payload} = \text{salt} + \text{":"} + \text{sessionId} + \text{":"} + \text{timestamp}$$
    $$\text{Token} = \text{Base64Url}(\text{Payload} + \text{":"} + \text{HmacSHA256}(\text{Payload}, \text{secretKey}))$$
  - Cryptographically verifies signature using constant-time equality (`MessageDigest.isEqual`), rejects expired tokens (> 7 days), and binds token strictly to the active user session ID.
  - Protects all state-mutating endpoints (`/api/v1/auth/refresh`, `/api/v1/auth/logout`, etc.).
- **Verification**: Verified by `SecurityHardeningIntegrationTest.shouldRejectForgedOrCrossSessionCsrfTokens` and `AuthControllerIntegrationTest`.

### Finding SEC-13: HMAC Verification Order & Query Canonicalization (Phase 11.1.1)
- **Severity**: HIGH
- **OWASP Category**: A02:2021 — Cryptographic Failures / Replay Attacks
- **Description**: Verifying or consuming nonces before cryptographic signature verification allows attackers to execute denial-of-service by burning valid nonces with forged signatures. Furthermore, non-canonical query strings could cause hash mismatches.
- **Remediation**:
  - In `ai-service/app/api/routes/internal/security.py`:
    1. Verify timestamp drift ($\le 300\text{s}$) first.
    2. Compute SHA-256 canonical hash (sorting query parameters alphabetically by key-value: `canonicalize_query`).
    3. **Verify HMAC signature BEFORE nonce consumption**.
    4. Only after cryptographic signature verification succeeds, check and register nonce atomically in Redis (`SET cs:replay:nonce:<nonce> 1 EX 360 NX`).
- **Verification**: Verified by `test_internal_security.py` in `ai-service` (`test_invalid_signature_does_not_consume_nonce`, `test_hmac_query_parameter_canonicalization`).

### Finding SEC-14: Refresh-Token Concurrency Isolation & Family Revocation (Phase 11.1.1 & Phase 11 Closure)
- **Severity**: HIGH
- **OWASP Category**: A01:2021 — Identification and Authentication Failures
- **Description**: Concurrent refresh requests on high-latency networks could race. Furthermore, original historical tokens (prior to V14) lacked parent/child pointers, making historical family reconstruction mathematically non-deterministic due to interleaved multi-device sessions. Under initial `family_id = id` backfill, replaying a legacy revoked token would isolate revocation to that token's synthetic family, risking leaving active successor sessions valid.
- **Remediation**:
  - Implemented migration `V15__harden_legacy_token_family_isolation.sql` tracking `is_legacy BOOLEAN NOT NULL DEFAULT FALSE`.
  - When an active legacy token rotates for the first time, it transitions out of legacy status by generating a fresh, isolated `family_id`.
  - **Defensive Legacy Replay Fallback**: If an already-rotated pre-migration legacy token is replayed, `AuthService.java` executes `refreshTokenRepository.revokeAllByUserId(storedToken.getUser().getId())`. This strictly guarantees that replay of a pre-migration revoked token cannot silently leave active successors valid.
  - Implemented strict concurrency handling: when concurrent refresh requests arrive with the same valid token, pessimistic write lock (`findByTokenHashForUpdate`) serializes execution—exactly one succeeds and others are safely rejected.
- **Verification**: Verified by `LegacyRefreshTokenMigrationIntegrationTest` (2/2 passing) and `SecurityHardeningIntegrationTest.shouldHandleConcurrentRefreshRequestsWithFamilyIsolation`.

### Finding SEC-15: Redis Rate-Limit Atomicity via Lua Scripting (Phase 11 Closure)
- **Severity**: HIGH
- **OWASP Category**: A04:2021 — Insecure Design / Rate Limiting
- **Description**: Two-step rate-limiting counters (`increment` followed by separate `expire`) risked orphaned keys with indefinite TTL (-1) if failures or crashes occurred between calls.
- **Remediation**:
  - Replaced two-step calls in `RateLimitingFilter.java` with an atomic Lua script (`ATOMIC_RATE_LIMIT_LUA`) executed on the Redis server:
    ```lua
    local current = redis.call('INCR', KEYS[1])
    if tonumber(current) == 1 then
        redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
    end
    return current
    ```
  - Guarantees key counter increment and TTL assignment execute atomically.
  - Rate limiting key TTL is preserved and never dropped under high-concurrency races.
- **Verification**: Verified by `RedisRateLimitAtomicityIntegrationTest` (3/3 passing, testing atomicity, 20-thread concurrency, and fail-closed store outages).

### Finding SEC-16: Production ClamAV Health Indicator & Readiness Probe (Phase 11 Closure)
- **Severity**: MEDIUM
- **OWASP Category**: A08:2021 — Software and Data Integrity Failures
- **Description**: Need explicit operational verification that ClamAV daemon container readiness is enforced before application traffic is routed, and daemon failure fails closed.
- **Remediation**:
  - Implemented `ClamAvHealthIndicator.java` for Spring Boot Actuator (`/actuator/health`). When `security.antivirus.clamav.required=true`, if the daemon is unreachable or disabled, health check status reports `DOWN` (`DISABLED_BUT_REQUIRED` or `UNAVAILABLE`), causing Kubernetes/Docker readiness probes to withhold traffic.
  - Added `clamav` container definition to `docker-compose.yml` (`clamav/clamav:latest`, port 3310, healthchecked with `clamdscan --ping`).
  - Stream inspection spools bounded files to temporary storage and inspects from byte 0 to EOF over `zINSTREAM\0` TCP socket, strictly failing closed on socket read timeouts.
- **Verification**: Verified by `ClamAvProductionReadinessIntegrationTest` (7/7 passing).

---

## 5. Ongoing Operational & Key Rotation Guidance

1. **HMAC Key Rotation Procedure**:
   - Step 1: Update the secondary accepted secret in Python AI service (`security.py`).
   - Step 2: Deploy updated `INTERNAL_SERVICE_KEY` to Spring Boot backend.
   - Step 3: Remove retired secret from Python AI service configuration.
2. **Redis Nonce Storage Sizing**:
   - Nonces expire automatically after 360 seconds (`TTL 360s`).
   - At 10,000 internal requests/minute, peak memory consumption in Redis is $< 5\text{ MB}$, with zero compaction overhead.
3. **ClamAV Antivirus Daemon in Production**:
   - In production environments requiring formal antivirus certification, configure `ClamAvMalwareScanner` (`security.antivirus.clamav.enabled=true`, `required=true`) pointing to a hardened containerized `clamd` instance on port 3310.
4. **Rate Limiting Failure Monitoring**:
   - Monitor `RateLimitingFilter.getRedisFailureCount()`. Alerts should trigger if `redisFailureCount > 0` to notify operators that high-cost AI inference is operating in fail-closed mode.
