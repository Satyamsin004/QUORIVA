# CS Scholar AI — Production Readiness & Operations Runbook

## 1. Overview & System Component Matrix

CS Scholar AI is structured as a decoupled microservices architecture designed for containerized deployment (e.g., Kubernetes, Docker Compose, or AWS ECS):

| Component | Technology | Primary Role | Default Port | Health / Probe |
| :--- | :--- | :--- | :--- | :--- |
| **Backend API** | Java 21 / Spring Boot 3.3.5 | Auth, Workspaces, Storage, RBAC | 8080 | `/actuator/health` |
| **AI Microservice** | Python 3.10 / FastAPI / Celery | Ingestion, Embeddings, RAG, Tutor | 8000 | `/health` |
| **Relational DB** | PostgreSQL 15 + pgvector 0.5.1 | Academic Data, Vector Index (HNSW) | 5432 | `pg_isready -h localhost` |
| **Object Storage** | MinIO / AWS S3 | File storage (PDF, DOCX, PPTX) | 9000 / 9001 | `/minio/health/live` |
| **Cache & Queue** | Redis 7.2 | Celery Broker, Result Backend | 6379 | `redis-cli ping` |
| **Frontend Web** | React 18 / TypeScript / Vite | Single Page Application UI | 5173 / 80 | Static HTTP 200 |

---

## 2. Environment Configuration & Secret Management

All sensitive secrets must be injected through environment variables or a secure secret manager (e.g., AWS Secrets Manager, HashiCorp Vault, Kubernetes Secrets). **Never commit defaults to source control.**

### 2.1 Spring Boot Backend Variables

| Variable | Description | Production Guidance |
| :--- | :--- | :--- |
| `SERVER_PORT` | HTTP port | `8080` |
| `POSTGRES_HOST` | Database host | RDS / Managed Postgres endpoint |
| `POSTGRES_PORT` | Database port | `5432` |
| `POSTGRES_DB` | Database schema name | `cs_scholar_db` |
| `POSTGRES_USER` | Database username | Dedicated least-privilege user |
| `POSTGRES_PASSWORD` | Database password | $\ge 32$ random characters |
| `REDIS_HOST` | Redis host | Managed Redis cluster |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis auth token | Complex secret token |
| `AI_SERVICE_URL` | AI Microservice base URL | Internal VPC URL (`http://ai-service:8000`) |
| `INTERNAL_SERVICE_KEY` | HMAC shared secret key | $\ge 64$ random hex characters |
| `JWT_SECRET` | JWT HS256/HS512 signing secret | $\ge 256$ bits random key |
| `JWT_ACCESS_EXPIRATION_MS` | Access token lifespan | `900000` (15 minutes) |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh token lifespan | `604800000` (7 days) |
| `JWT_COOKIE_SECURE` | Enforce Secure flag on cookies | `true` (HTTPS in production) |
| `JWT_COOKIE_SAME_SITE` | Cookie SameSite policy | `Lax` (or `Strict`) |
| `RATE_LIMITING_ENABLED` | Enable rate limiting filter | `true` |
| `TRUSTED_PROXIES` | Comma-separated trusted proxies | VPC Load Balancer IPs / CIDRs |
| `S3_ENDPOINT` | Object storage endpoint | `https://s3.<region>.amazonaws.com` |
| `S3_REGION` | S3 region | Deployment region (e.g., `us-east-1`) |
| `S3_ACCESS_KEY` | S3 IAM access key | Least-privilege IAM service user |
| `S3_SECRET_KEY` | S3 IAM secret key | IAM secret key |
| `S3_BUCKET` | Main document bucket | `cs-scholar-documents-prod` |
| `S3_PATH_STYLE_ACCESS` | S3 path style flag | `false` for AWS S3; `true` for MinIO |
| `CLAMAV_HOST` | ClamAV antivirus daemon host | Internal container/daemon IP |
| `CLAMAV_PORT` | ClamAV antivirus daemon port | `3310` |

### 2.2 Python AI Service Variables

| Variable | Description | Production Guidance |
| :--- | :--- | :--- |
| `DATABASE_URL` | SQLAlchemy async/sync DB URL | `postgresql://user:pass@host:5432/db` |
| `REDIS_URL` | Celery broker URL | `redis://:pass@host:6379/0` |
| `INTERNAL_SERVICE_KEY` | HMAC shared secret key | Must match Spring Boot's key |
| `LLM_PROVIDER` | LLM backend (`gemini`, `openai`, `mock`) | Production: `gemini` or `openai` |
| `GEMINI_API_KEY` | Google Gemini API secret key | Injected securely |
| `OPENAI_API_KEY` | OpenAI API secret key | Injected securely if using OpenAI |

### 2.3 Internal HMAC Key Rotation & Replay Protection

#### Distributed Replay Nonce Cache (Redis)
- Spring Boot generates a unique UUID nonce for every internal request to the Python AI service.
- The Python AI service checks and stores each nonce in Redis: `SET cs:replay:nonce:<nonce> 1 EX 360 NX`.
- Nonces automatically expire after 360 seconds (covering the 300-second timestamp drift window).
- Memory footprint is negligible: at 10,000 requests/minute, active keys require $< 5\text{ MB}$ of Redis RAM.
- **Fail-Closed Policy**: If Redis is unreachable, the AI service rejects internal requests with HTTP 503 Service Unavailable to prevent unverified replay attacks.

#### Zero-Downtime HMAC Key Rotation Procedure
1. **Prepare New Secret**: Generate a new cryptographically random 64-character hex secret.
2. **Update AI Service**: Configure the Python AI service to accept both the new secret and the previous secret during the grace period.
3. **Deploy Backend**: Update `INTERNAL_SERVICE_KEY` in the Spring Boot backend environment and redeploy.
4. **Retire Old Secret**: Remove the legacy secret from the Python AI service and restart workers.

---

## 3. Database & S3 Backup and Restore Runbook

### 3.1 PostgreSQL Database Backup Runbook
Perform automated daily snapshots and continuous WAL archiving (e.g. AWS RDS automated backups or `pg_dump` cron):

```bash
# Snapshot logical backup
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
pg_dump -h $POSTGRES_HOST -p $POSTGRES_PORT -U $POSTGRES_USER \
  -F c -b -v -f "/backups/cs_scholar_db_${TIMESTAMP}.dump" $POSTGRES_DB

# Point-in-time restore procedure
pg_restore -h $POSTGRES_HOST -p $POSTGRES_PORT -U $POSTGRES_USER \
  -d $POSTGRES_DB --clean --if-exists "/backups/cs_scholar_db_${TIMESTAMP}.dump"
```

### 3.2 S3 / MinIO Object Storage Backup Runbook
Enable **S3 Object Versioning** and **Cross-Region Replication (CRR)** on the production bucket.

For standalone MinIO deployments:
```bash
# Periodic sync using MinIO Client (mc)
mc mirror --overwrite --remove localminio/cs-scholar-documents backup-s3/cs-scholar-documents-backup
```

---

## 4. Disaster Recovery & Failover Plan

| Disaster Scenario | Recovery Objective | Procedure |
| :--- | :--- | :--- |
| Database Corruption / Loss | RPO < 15 min, RTO < 30 min | Restore latest WAL snapshot from RDS automated backups or run `pg_restore`. |
| S3 Storage Outage | RPO < 5 min, RTO < 10 min | Failover DNS/endpoint to cross-region replica bucket. |
| Redis Crash / Data Loss | RPO: Ephemeral, RTO < 5 min | High-cost AI generation and tutor endpoints fail closed (HTTP 503) to prevent unmetered LLM quota exhaustion; Standard CRUD degrades to in-memory sliding window; Celery tasks re-queue; restart Redis instance. |
| AI Service Overload | RTO < 2 min | Auto-scale FastAPI deployment (HPA based on CPU/Request count). |

---

## 5. Security Checklist Before Public Launch

- [x] All default passwords changed in production environment.
- [x] Spring Boot Actuator `/actuator/**` restricted (only `ADMIN` role can access metrics/env).
- [x] Spring Boot and Python AI service communicate using signed HMAC-SHA256 requests with canonicalized queries, timestamp skew check ($\le 300\text{s}$), signature verified **before** nonce consumption, and distributed Redis replay protection.
- [x] Refresh tokens delivered exclusively via `HttpOnly`, `SameSite=Lax` cookies, omitted from JSON bodies, protected by cryptographically signed session-bound Double-Submit CSRF tokens (`CsrfTokenService`).
- [x] Refresh token reuse triggers family-scoped invalidation (`family_id`) with concurrent race protection.
- [x] Distributed rate limiting active on `/api/v1/auth/**` and `/api/v1/learning/**/generate` with proxy validation and risk-based fail-closed policy during Redis outages.
- [x] File upload scanner operates in fail-closed mode with complete-file multi-engine scanning (`CompositeMalwareScanner` chaining ClamAV and full-stream sliding window heuristic scanner; byte 0 to EOF).
- [x] `X-Frame-Options: DENY` and `X-Content-Type-Options: nosniff` headers enforced.
- [x] CORS restricted to verified production frontend domain(s).
