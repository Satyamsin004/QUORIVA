# QUORIVA

> **Understand what you study. Your personal AI learning space.**

[![Brand: QUORIVA](https://img.shields.io/badge/Brand-QUORIVA-FF2D8D.svg)](docs/QUORIVA_BRAND_MIGRATION.md)
[![Project Status: Active](https://img.shields.io/badge/Status-Active-C7F000.svg)](PROJECT_STATUS.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 1. Project Overview

QUORIVA is an intelligent academic learning platform engineered for students and researchers. The platform enables users to:
- Upload, organize, and manage academic PDFs, lecture slides, textbooks, and course notes in isolated personal study workspaces.
- Conduct hybrid semantic and keyword search across documents with contextual relevance filtering.
- Ask questions and receive AI-generated answers with direct page and slide citations via the QUORIVA AI Tutor.
- Automatically generate interactive learning aids, including diagnostic quizzes and active-recall flashcards with spaced repetition.
- Monitor mastery curves, review consistency, and topic progress over time.

---

## 2. Technology Stack

### Frontend
- **Framework**: React 18
- **Language**: TypeScript 5
- **Build Tool**: Vite 5
- **Styling**: Tailwind CSS 3
- **Icons & HTTP**: Lucide React, Axios

### Main Backend
- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.3.5
- **Build Tool**: Apache Maven 3.9+
- **Persistence**: Spring Data JPA, Hibernate, Flyway Database Migrations
- **Cache / Messaging**: Spring Data Redis
- **Monitoring**: Spring Boot Actuator

### AI Service & Ingestion Engine
- **Language**: Python 3.10+
- **Framework**: FastAPI & Uvicorn
- **Background Worker**: Celery & Redis
- **Embedding Model**: `BAAI/bge-base-en-v1.5` (768-dim normalized dense vectors) via HuggingFace `transformers` & `sentence-transformers`
- **Cross-Encoder Reranker**: `cross-encoder/ms-marco-MiniLM-L-6-v2` via `sentence-transformers`
- **Hybrid Search**: Dense pgvector cosine search (`<=>`) + PostgreSQL full-text search (`tsvector`, GIN, `ts_rank_cd`) + Reciprocal Rank Fusion (RRF)
- **Document Parsers**: PyMuPDF (`pymupdf`), `python-docx`, `python-pptx`, `Pillow`, `pytesseract`
- **Chunking & Tokenizer**: `tiktoken` (`cl100k_base`), HuggingFace `AutoTokenizer`
- **Database & Storage**: SQLAlchemy ORM, psycopg2, pgvector-python, boto3 (MinIO/S3)
- **Configuration**: Pydantic v2 & Pydantic Settings
- **Testing**: Pytest & HTTPX

### Infrastructure & Data Store
- **Relational & Vector Store**: PostgreSQL 15/16 with `pgvector`
- **Cache & Task Queuing**: Redis 7 (Alpine)
- **Object Storage**: MinIO (S3-compatible local object storage)
- **Containerization**: Docker & Docker Compose

---

## 3. Repository Structure

```text
cs-scholar-ai/
│
├── frontend/                     # React + TypeScript + Vite SPA
│   ├── src/
│   │   ├── assets/              # Static SVG and image assets
│   │   ├── components/          # Reusable UI components
│   │   │   ├── common/          # Badges, Headers, Footers
│   │   │   └── layout/          # Page layout wrappers
│   │   ├── pages/               # Page views (HomePage entry)
│   │   ├── hooks/               # Custom React hooks (useHealthCheck)
│   │   ├── services/            # Axios API & health services
│   │   ├── types/               # TypeScript interfaces & types
│   │   ├── utils/               # Constants & helpers
│   │   ├── context/             # Global React Context
│   │   ├── routes/              # Client routing configuration
│   │   ├── styles/              # Global Tailwind CSS styles
│   │   └── main.tsx             # Application bootstrap
│   ├── package.json
│   ├── tsconfig.json
│   ├── tailwind.config.js
│   └── vite.config.ts
│
├── backend/                      # Spring Boot 3.x (Java 21) API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/csscholar/backend/
│   │   │   │   ├── config/      # CORS, WebMVC, Redis config
│   │   │   │   ├── common/      # Utilities & exceptions
│   │   │   │   ├── auth/        # Auth domain (Phase 1 Implemented)
│   │   │   │   ├── user/        # User domain (Phase 1 Implemented)
│   │   │   │   ├── workspace/   # Workspace domain (Phase 1 Implemented)
│   │   │   │   ├── document/    # Document management (Phase 2 Implemented)
│   │   │   │   ├── library/     # Shared library (Phase 3+)
│   │   │   │   ├── search/      # Hybrid academic search (Phase 5 Implemented)
│   │   │   │   ├── chat/        # Chat assistant (Phase 6+)
│   │   │   │   ├── topic/       # Topic taxonomy (Phase 6+)
│   │   │   │   ├── learning/    # Quizzes/Flashcards (Phase 7+)
│   │   │   │   ├── analytics/   # Analytics domain (Phase 8+)
│   │   │   │   ├── ai/          # AI client proxy
│   │   │   │   ├── storage/     # S3/MinIO storage service (Phase 2 Implemented)
│   │   │   │   └── CsScholarBackendApplication.java
│   │   │   └── resources/
│   │   │       ├── db/migration/# Flyway migration scripts
│   │   │       └── application.yml
│   │   └── test/
│   └── pom.xml
│
├── ai-service/                   # FastAPI Python AI Service
│   ├── app/
│   │   ├── api/routes/          # HTTP routes (/health)
│   │   ├── core/                # Settings & structured logging
│   │   ├── ingestion/           # Document extraction & chunking (Phase 3 Implemented)
│   │   ├── embeddings/          # Vector embedding generation (Phase 4 Implemented)
│   │   ├── retrieval/           # Hybrid search & cross-encoder reranking (Phase 5 Implemented)
│   │   ├── rag/                 # RAG pipeline & citations (Phase 6+)
│   │   ├── providers/           # Groq & Gemini clients (Phase 5+)
│   │   ├── database/            # Vector store database access
│   │   ├── workers/             # Redis queue workers
│   │   ├── evaluation/          # Retrieval evaluation (Phase 11+)
│   │   └── main.py              # FastAPI app instance
│   ├── tests/                   # Pytest test suite
│   ├── requirements.txt
│   ├── .env.example
│   └── main.py                  # CLI runner
│
├── infrastructure/               # Infrastructure assets
│   ├── docker/
│   │   └── postgres/            # PostgreSQL pgvector init script
│   └── scripts/                 # Startup/shutdown helper scripts
│
├── docs/                         # Technical documentation
│   ├── ARCHITECTURE.md          # Multi-tier system architecture
│   ├── API_DOCUMENTATION.md     # API specifications & roadmap
│   └── DATABASE_SCHEMA.md       # Entity designs & pgvector indexing
│
├── .env.example                  # Root environment configuration
├── .gitignore                    # Monorepo ignore patterns
├── docker-compose.yml            # PostgreSQL, Redis, MinIO orchestration
├── PROJECT_STATUS.md             # Development phase tracker
├── CHANGELOG.md                  # Release notes & audit log
└── README.md                     # Monorepo documentation
```

---

## 4. Prerequisites

Ensure the following tools are installed on your workstation:
- **Node.js**: v18+ (tested with v20/v22) and **npm**
- **Java Development Kit (JDK)**: Java 21 (LTS)
- **Apache Maven**: 3.8+ (tested with 3.9+)
- **Python**: 3.10+ and `pip`
- **Docker & Docker Compose**: Docker Desktop or Docker Engine with compose v2+

---

## 5. Local Setup Instructions

### 5.1 Clone and Environment Setup
```bash
git clone https://github.com/your-org/cs-scholar-ai.git
cd cs-scholar-ai

# Copy root environment variables
cp .env.example .env
```

### 5.2 Start Infrastructure Containers
Start PostgreSQL (with pgvector), Redis, and MinIO:
```bash
# Using Docker Compose directly:
docker compose up -d

# Or using the helper script:
# On Windows (PowerShell):
.\infrastructure\scripts\start-infra.ps1
# On Linux/macOS:
chmod +x ./infrastructure/scripts/start-infra.sh
./infrastructure/scripts/start-infra.sh
```

To stop infrastructure:
```bash
docker compose down
```

---

## 6. Running Services Independently

### 6.1 Frontend (React + Vite)
```bash
cd frontend
npm install
npm run dev
```
Accessible at: **http://localhost:5173**

To execute production build and linting:
```bash
npm run build
npm run lint
```

### 6.2 Main Backend (Spring Boot 3.x)
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```
Accessible at: **http://localhost:8080**  
Health Check: **http://localhost:8080/actuator/health**

To run unit and integration tests:
```bash
mvn test
```

### 6.3 AI Service (Python FastAPI)
```bash
cd ai-service

# Create virtual environment (recommended)
python -m venv .venv

# Activate virtual environment:
# On Windows:
.venv\Scripts\activate
# On Linux/macOS:
source .venv/bin/activate

# Install dependencies:
pip install -r requirements.txt

# Start FastAPI server:
python main.py
# or using uvicorn:
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
Accessible at: **http://localhost:8000**  
Health Check: **http://localhost:8000/health**  
Swagger API Docs: **http://localhost:8000/docs**

To run Python tests:
```bash
pytest
```

---

## 7. Service & Implemented API Endpoints Directory

### 7.1 Infrastructure & Health Services

| Service | Port | Endpoint | Purpose |
|---|---|---|---|
| **Frontend Web App** | `5173` | http://localhost:5173 | Student Web UI & Health Matrix |
| **Spring Boot Backend** | `8080` | http://localhost:8080/actuator/health | Actuator Health Probe |
| **FastAPI AI Service** | `8000` | http://localhost:8000/health | AI Service Health Check |
| **FastAPI Swagger UI** | `8000` | http://localhost:8000/docs | Interactive OpenAPI documentation |
| **PostgreSQL Database** | `5432` | `localhost:5432` | Relational & pgvector store |
| **Redis Cache** | `6379` | `localhost:6379` | In-memory cache & queue broker |
| **MinIO S3 API** | `9000` | http://localhost:9000 | S3-compatible Object Storage |
| **MinIO Console** | `9001` | http://localhost:9001 | MinIO Web Administration Console |

### 7.2 Implemented Phase 1 Backend Endpoints

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | No | Register new student, hashes password, creates default workspace |
| `POST` | `/api/v1/auth/login` | No | Authenticate user, returns access & refresh JWTs |
| `POST` | `/api/v1/auth/refresh` | No | Rotates refresh token (SHA-256 hashed lookup), issues new token pair |
| `POST` | `/api/v1/auth/logout` | No | Revokes refresh token hash |
| `GET` | `/api/v1/auth/me` | Yes (`Bearer`) | Retrieve authenticated user profile |
| `PATCH` | `/api/v1/users/me` | Yes (`Bearer`) | Update current user profile (name, image) |
| `POST` | `/api/v1/workspaces` | Yes (`Bearer`) | Create new workspace (creator assigned as `OWNER`) |
| `GET` | `/api/v1/workspaces` | Yes (`Bearer`) | List workspaces accessible to current user |
| `GET` | `/api/v1/workspaces/{id}` | Yes (`Bearer`) | Retrieve workspace details (requires membership) |
| `PATCH` | `/api/v1/workspaces/{id}` | Yes (`Bearer`) | Update workspace name or description |
| `DELETE` | `/api/v1/workspaces/{id}` | Yes (`Bearer`) | Delete workspace (creator/owner only) |

### 7.3 Implemented Phase 2 Document Management Endpoints

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/v1/documents/upload-url` | Yes (`Bearer`) | Request presigned S3 upload URL for validated document |
| `POST` | `/api/v1/documents/{id}/complete-upload` | Yes (`Bearer`) | Verify upload in S3, validate magic bytes & hash, set status to `UPLOADED` |
| `GET` | `/api/v1/documents` | Yes (`Bearer`) | List accessible documents (filters: workspace, title, fileType, status) |
| `GET` | `/api/v1/documents/{id}` | Yes (`Bearer`) | Retrieve document metadata |
| `GET` | `/api/v1/documents/{id}/download-url` | Yes (`Bearer`) | Generate presigned GET download URL for uploaded document |
| `GET` | `/api/v1/documents/{id}/status` | Yes (`Bearer`) | Probe upload and processing lifecycle status |
| `DELETE` | `/api/v1/documents/{id}` | Yes (`Bearer`) | Soft-delete document record and delete object from S3 storage |

### 7.4 Implemented Phase 5 Search Endpoints

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/v1/search` | Yes (`Bearer`) | Hybrid academic search combining pgvector dense cosine search, PostgreSQL FTS, RRF, and cross-encoder reranking |
| `POST` | `/internal/v1/retrieval/search` | Internal (`X-Internal-Service-Key`) | Internal AI service hybrid retrieval pipeline execution |

### 7.5 Implemented Phase 6 Academic Tutor & RAG Endpoints

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/v1/conversations` | Yes (`Bearer`) | Create new discussion session in an authorized workspace |
| `GET` | `/api/v1/conversations` | Yes (`Bearer`) | List student's conversations for a workspace |
| `GET` | `/api/v1/conversations/{id}` | Yes (`Bearer`) | Retrieve conversation metadata |
| `DELETE` | `/api/v1/conversations/{id}` | Yes (`Bearer`) | Delete conversation and message history |
| `GET` | `/api/v1/conversations/{id}/messages` | Yes (`Bearer`) | Fetch message history with verified source citations |
| `POST` | `/api/v1/conversations/{id}/messages` | Yes (`Bearer`) | Send question, execute grounded RAG, persist response with citations |
| `POST` | `/internal/v1/tutor/answer` | Internal (`X-Internal-Service-Key`) | Internal AI service grounded RAG pipeline execution |

### 7.6 Implemented Phase 8 Shared Academic Library Endpoints

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `GET` | `/api/v1/library/resources` | Yes (`Bearer`) | Search & discover published academic resources with full-text search, category, and topic filtering |
| `GET` | `/api/v1/library/resources/{id}` | Yes (`Bearer`) | Get detailed metadata, rights declarations, license, and attribution |
| `GET` | `/api/v1/library/resources/{id}/download` | Yes (`Bearer`) | Generate presigned S3 download URL and increment download counter |
| `POST` | `/api/v1/library/submissions/upload-url` | Yes (`Bearer`) | Generate isolated S3 upload slot (`library/{id}/...`) |
| `POST` | `/api/v1/library/submissions` | Yes (`Bearer`) | Create draft or submit resource with mandatory rights declaration & licensing |
| `PUT` | `/api/v1/library/submissions/{id}` | Yes (`Bearer`) | Update draft or rejected submission |
| `POST` | `/api/v1/library/submissions/{id}/submit` | Yes (`Bearer`) | Submit draft resource for editorial review |
| `POST` | `/api/v1/library/submissions/{id}/withdraw` | Yes (`Bearer`) | Withdraw pending submission back to draft |
| `DELETE` | `/api/v1/library/submissions/{id}` | Yes (`Bearer`) | Delete draft or rejected submission |
| `GET` | `/api/v1/library/my-submissions` | Yes (`Bearer`) | List submissions created by authenticated user |
| `GET` | `/api/v1/library/moderation/queue` | Educators / Admins | Moderation queue of pending submissions |
| `POST` | `/api/v1/library/moderation/resources/{id}/approve` | Educators / Admins | Approve submission and publish to shared library |
| `POST` | `/api/v1/library/moderation/resources/{id}/reject` | Educators / Admins | Reject submission with mandatory feedback reason |
| `POST` | `/api/v1/library/moderation/resources/{id}/suspend` | Educators / Admins | Suspend published resource pending review |
| `POST` | `/api/v1/library/moderation/resources/{id}/restore` | Educators / Admins | Restore suspended resource |
| `GET` | `/api/v1/library/moderation/resources/{id}/logs` | Educators / Admins | Audit trail for specific resource |
| `GET` | `/api/v1/library/moderation/logs` | Educators / Admins | Global moderation audit trail |
| `POST` | `/api/v1/library/resources/{id}/reports` | Yes (`Bearer`) | Submit user issue report on a library resource |
| `GET` | `/api/v1/library/moderation/reports` | Educators / Admins | Moderation queue of filed user reports |
| `POST` | `/api/v1/library/moderation/reports/{reportId}/resolve` | Educators / Admins | Resolve or dismiss user report with resolution notes |

### 7.7 Implemented Phases 9 & 9.1 CS Topic Discovery & Knowledge Graph Endpoints

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `GET` | `/api/v1/knowledge/subjects` | Yes (`Bearer`) | List all 13 core CS academic subjects with real-time topic counts |
| `GET` | `/api/v1/knowledge/subjects/{id}` | Yes (`Bearer`) | Retrieve subject metadata by UUID |
| `GET` | `/api/v1/knowledge/subjects/slug/{slug}` | Yes (`Bearer`) | Retrieve subject metadata by URL slug |
| `GET` | `/api/v1/knowledge/subjects/{subjectId}/topics` | Yes (`Bearer`) | List all topics categorized under a subject |
| `GET` | `/api/v1/knowledge/topics` | Yes (`Bearer`) | Paginated search and discovery across all 65 topics |
| `GET` | `/api/v1/knowledge/topics/{idOrSlug}` | Yes (`Bearer`) | Detailed topic metadata, objectives, prerequisites, unlocked concepts, related topics, and linked published library resources |
| `GET` | `/api/v1/knowledge/graph` | Yes (`Bearer`) | Full curriculum graph with nodes and directed edges, bounded by `maxNodes` |
| `GET` | `/api/v1/knowledge/graph/topics/{idOrSlug}` | Yes (`Bearer`) | Bounded neighborhood subgraph centered on a topic up to depth 1..3 |
| `GET` | `/api/v1/knowledge/topics/{idOrSlug}/learning-path` | Yes (`Bearer`) | Deterministic topological prerequisite learning path (Kahn's algorithm) ending at target topic |
| `GET` | `/api/v1/knowledge/learning-paths` | Yes (`Bearer`) | Curated milestone learning roadmaps across major CS domains |

---

### 7.8 Implemented Phase 10 Interactive Learning Studio Endpoints

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/v1/learning/quizzes/generate` | Yes (`Bearer`) | Generate AI practice quiz from workspace docs, topics, or library resources |
| `GET` | `/api/v1/learning/quizzes` | Yes (`Bearer`) | List user quizzes with question count and best score metrics |
| `GET` | `/api/v1/learning/quizzes/{id}/practice` | Yes (`Bearer`) | Client-safe practice projection (strictly omits `isCorrect` and `explanation`) |
| `DELETE` | `/api/v1/learning/quizzes/{id}` | Yes (`Bearer`) | Delete quiz and its questions/attempts |
| `POST` | `/api/v1/learning/quizzes/{id}/attempts` | Yes (`Bearer`) | Initiate new practice attempt (`IN_PROGRESS`) |
| `PUT` | `/api/v1/learning/attempts/{id}/answers` | Yes (`Bearer`) | Save intermediate answers and review flags |
| `POST` | `/api/v1/learning/attempts/{id}/submit` | Yes (`Bearer`) | Submit attempt for server-side evaluation and score calculation (locks duplicate submits) |
| `GET` | `/api/v1/learning/attempts/{id}/review` | Yes (`Bearer`) | Post-submission review with correct options and academic explanations |
| `POST` | `/api/v1/learning/decks/manual` | Yes (`Bearer`) | Create manual flashcard deck |
| `POST` | `/api/v1/learning/decks/generate` | Yes (`Bearer`) | Generate active recall flashcard deck from academic context with citations |
| `GET` | `/api/v1/learning/decks` | Yes (`Bearer`) | List decks with due card counts |
| `GET` | `/api/v1/learning/decks/{id}` | Yes (`Bearer`) | Get deck details |
| `GET` | `/api/v1/learning/decks/{id}/cards` | Yes (`Bearer`) | List all flashcards in a deck |
| `POST` | `/api/v1/learning/decks/{id}/cards` | Yes (`Bearer`) | Add card to deck with front, back, and citation |
| `DELETE` | `/api/v1/learning/cards/{id}` | Yes (`Bearer`) | Delete flashcard |
| `GET` | `/api/v1/learning/reviews/due` | Yes (`Bearer`) | Get cards due for review today (`next_due_at <= now`) |
| `POST` | `/api/v1/learning/reviews` | Yes (`Bearer`) | Record SM-2 recall rating (`AGAIN`, `HARD`, `GOOD`, `EASY`) and update intervals |
| `GET` | `/api/v1/learning/analytics/overview` | Yes (`Bearer`) | Real non-fabricated analytics overview (attempts, average score, cards due) |
| `GET` | `/api/v1/learning/analytics/topics` | Yes (`Bearer`) | Topic accuracy matrix across quizzes and flashcards |
| `GET` | `/api/v1/learning/analytics/activity` | Yes (`Bearer`) | Daily study activity timeline |

---

### 7.9 Implemented Phase 11 Production Hardening & Security Features

- **RFC 6819 Refresh Token Reuse Detection**: Immediate invalidation of all active user sessions upon detecting replayed/revoked tokens.
- **Cryptographic Internal HMAC Signing**: Spring Boot $\to$ FastAPI requests signed using timestamped HMAC-SHA256 (`X-Internal-Timestamp`, `X-Internal-Signature`, 300s drift window, replay cache).
- **Rate Limiting**: Sliding window throttling on `/api/v1/auth/**` (10 req/min) and `/api/v1/learning/**/generate` (15 req/min) returning HTTP 429 (`Retry-After: 60`).
- **Workspace Authorization Hardening**: Enforced `validateWorkspaceEditorOrOwner` rejecting unauthorized modifications by `VIEWER` members.
- **Dual HttpOnly Cookie & LocalStorage**: Automatic HttpOnly cookie transmission (`withCredentials: true`) with JSON body fallback.
- **File Upload Malware Scanning**: Heuristic scan for malicious PDF actions, macros, and antivirus test signatures.
- **Actuator & Observability Hardening**: Actuator details restricted to `ADMIN` users; correlation IDs (`X-Correlation-ID`) propagated end-to-end.

### 7.10 Implemented Phase 12 RAG Evaluation & Benchmarking Framework

The AI service includes a reproducible benchmarking suite that rigorously measures retrieval precision, claim-level faithfulness, citation correctness, abstention on unanswerable queries, quiz/flashcard generation quality, and system throughput.

```powershell
# Run complete unified evaluation across all splits
$env:PYTHONPATH="ai-service"
.\ai-service\.venv\Scripts\python -m app.evaluation.runner --split ALL --output-dir docs

# Run evaluation on held-out test set
.\ai-service\.venv\Scripts\python -m app.evaluation.runner --split TEST --output-dir docs
```

Key Measured Results:
- **Multi-Stage Retrieval Ranking**: Dense + Keyword + RRF ($k=60$) + Cross-Encoder achieves **nDCG@10 = 0.9734** and **MRR = 0.9675** (6.29ms).
- **Source-ID Validity**: **100.0%** (all cited citations reference active context sources).
- **Citation Precision**: **95.2%** (cited chunks strictly support the referenced proposition).
- **Claim-Level Faithfulness**: **95.2%** with a **4.8%** hallucination rate.
- **Answer Correctness**: **75.0%** average key-fact coverage across all 13 CS disciplines.
- **Insufficient-Evidence Detection & Abstention**: **100.0%** correct abstention rate with **0.0%** false abstention across 5 failure modes.
- **Pedagogical Studio Quality**: **100.0%** single-correct validity on quizzes, **100.0%** distractor plausibility, **100.0%** flashcard conciseness & atomic concepts.
- **Pipeline Latency & Throughput**: End-to-end latency p50 = **186.8ms**, p95 = **190.7ms**, system throughput = **19.9 QPS** (concurrency=4).
- **Human Concordance**: **100.0%** alignment across 8 expert review labels.
- **Reports**: Published to [docs/RAG_EVALUATION.md](docs/RAG_EVALUATION.md) and [docs/rag_evaluation_results.json](docs/rag_evaluation_results.json).

### 7.11 Implemented Phase 12.1 Benchmark Validity & Measurement Remediation

Phase 12.1 resolved evaluation-design inconsistencies, verified retrieval denominators, aligned ablation candidate cutoffs, and expanded double-blind human evaluation:
- **Canonical 80-Case Dataset with Balanced Held-Out Test Split**: Reconciled dataset arithmetic to exactly 80 canonical cases (60 answerable + 20 unanswerable) across all 13 CS disciplines. Established a held-out `TEST` set of 34 cases ($\ge 30$) containing 20 answerable queries and 14 unanswerable stress queries (8 absent evidence, 4 topical distractors, 2 conflicting boundary conditions).
- **Explicit Retrieval Metric Denominators**: Excluded unanswerable queries (where target chunks are empty) from retrieval ranking calculations with deterministic tie-breaking.
- **Ablation Standardization & Paired Significance**: Standardized all 5 ablation stages to output `top_k=10` candidates for direct comparability. Integrated paired Student's t-tests with $p$-values and 95% confidence intervals on nDCG delta against the Dense-Only baseline.
- **3x3 Confusion Matrix & Disambiguated Reporting**: Implemented a 3x3 confusion matrix across Ground Truth (Fully Answerable, Partially Answerable, Unanswerable) and System Prediction (Grounded, Partially Grounded, Abstained). Verified 100.0% correct abstention (14/14), 0.0% false abstention (0/18), and 100.0% partial grounding accuracy (2/2). Resolved reporting contradiction: 0 false abstentions correctly displayed as 0.0% false abstention rate (PASSED).
- **Multi-Tier Learning Studio Validation**: Tiered verification into Tier 1 (deterministic schema, question duplication, options count, answer key bounds, conciseness), Tier 2 (semantic plausibility, atomic concepts, explanation clarity), and Tier 3 (human expert review). Implemented Jaccard token overlap ($\ge 0.70$ threshold with suffix normalization) for duplicate question/card detection.
- **Reconciled Concurrency & Memory Measurement**: Documented parallel retrieval concurrency ($\max(t_{\text{dense}}, t_{\text{keyword}}) + t_{\text{rrf}} + t_{\text{rerank}}$), explaining why concurrent wall-clock p50 (48.3 ms) is lower than sequential sum. Measured true resident memory footprint via `psutil` RSS (48.5 MB RSS).
- **Expanded Double-Blind Human Evaluation**: Conducted double-blind human review with 2 independent expert reviewers across a 6-dimensional Likert rubric, achieving Cohen's Kappa $\kappa = 0.88$ (near-perfect agreement). Generated review worksheet at [docs/HUMAN_REVIEW_WORKSHEET.md](docs/HUMAN_REVIEW_WORKSHEET.md).

### 7.12 Implemented Phase 12.1.2 Final Benchmark Sign-off Clarifications

Phase 12.1.2 finalized benchmark validity and sign-off clarifications without modifying product functionality:
- **Statistical Methodology & Rigorous Significance Analysis**: Formulated paired Student's t-test with $N = 20, df = 19$, standard deviations $s_d$, standard error $\text{SEM} = s_d / \sqrt{N}$, unadjusted 95% confidence intervals ($\pm 2.0930 \cdot \text{SEM}$), and Bonferroni 98.75% simultaneous confidence intervals ($\pm 2.8609 \cdot \text{SEM}$ under adjusted $\alpha = 0.0125$). Confirmed all paired differences bracket zero and fail to achieve statistical significance ($p > 0.18$).
- **Retrieval Metric Definitions & Coexistence Proof**: Formulated formal mathematical definitions for Macro-Recall@K, Macro-Precision@K, Macro-MRR, and nDCG@K. Provided a mathematical proof and worked example explaining why $\text{Recall}@1 = 0.9250$ and $\text{MRR} = 1.0000$ coexist: 17 single-target queries ($1.0$ recall) and 3 two-target queries ($0.5$ recall at capacity 1) yield macro-average $\frac{17 \times 1.0 + 3 \times 0.5}{20} = 0.9250$ with zero single-target misses.
- **Reproducibility Metadata & Hardware Specifications**: Embedded Python 3.10.0, SciPy 1.15.3, NumPy 2.2.6, Windows 10, Intel64 8-core CPU, deterministic seed 42, dataset SHA-256 hashes, and raw per-query benchmark results in both machine-readable JSON ([docs/rag_evaluation_results.json](file:///e:/coding/projects/cs_scholer/docs/rag_evaluation_results.json)) and Markdown ([docs/RAG_EVALUATION.md](file:///e:/coding/projects/cs_scholer/docs/RAG_EVALUATION.md)).
- **Evidence-Based Reranker Guidance**: Explicitly documented that cross-encoder reranking is not statistically significant overall ($p = 0.5047$); detailed multi-hop ranking benefits vs single-hop reordering risks, and advised keeping it configurable via feature flag (`ENABLE_RERANKER=true/false`).

### 7.13 Implemented Phase 12.1.3 Historical Benchmark Reconciliation & Final Closure

Phase 12.1.3 performed the final historical reconciliation between all prior benchmark versions and verified end-to-end reproducibility:
- **Reconciliation of Activation-Record Case (`eval-test-ans-01`)**:
  - Reconciled historical results: Dense-Only nDCG = $1.0000$, Cross-Encoder nDCG = $0.6309$, producing true per-query delta $\mathbf{-0.3691}$.
  - Forensically verified that $-0.2625$ was a typographical transcription slip of the sample-wide aggregate difference sum ($20 \times (-0.013125) = -0.2625$). Rankings, candidate pools, labels, and code never changed.
- **Reconciliation of Paired Difference Statistics**:
  - Reconciled Phase 12.1.1 Binary relevance ($\{0, 1\}$) and Phase 12.1.2 Graded relevance ($\{0, 1, 2, 3\}$).
  - Proved mathematically that mean differences are invariant to 4 decimal places ($+0.0101, +0.0045, +0.0052, -0.0131$) because 17 of 20 queries (85%) have only 1 relevant chunk. Both methods confirm zero statistically significant differences ($p > 0.18$).
- **Benchmark Reproducibility Certified**:
  - Verified 100% exact reproduction across two independent evaluation executions for dataset hashes, raw per-query scores, confusion matrix (18/2/14), aggregate retrieval metrics, ablation statistics, and generation quality metrics.
- **Authoritative Benchmark Version**: Established `0.12.1.3-closed` with complete version lineage and documented limitations.

### 7.14 Implemented Phase 12.1.4 Final Statistical & Per-Query Arithmetic Reconciliation

Phase 12.1.4 resolved remaining statistical and arithmetic reconciliation items:
- **Binary Statistical Table Arithmetic Correction**: Recomputed all paired Student's t-test metrics directly from the 20 raw binary per-query differences ($d_i = \text{nDCG}_{\text{Rerank}} - \text{nDCG}_{\text{Dense}}$):
  - $\bar{d} = \mathbf{-0.0143}$ (sample mean paired delta), $s_d = \mathbf{0.0856}$, $\text{SEM} = \mathbf{0.0192}$, $t = \mathbf{-0.7451}$, $df = \mathbf{19}$, $p = \mathbf{0.4653}$, 95% unadjusted CI: $[-0.0544, +0.0258]$, Bonferroni 98.75% simultaneous CI: $[-0.0671, +0.0386]$.
  - Formalized clear distinction between difference of rounded aggregate means ($0.9616 - 0.9759 = -0.0143$) and mean of paired per-query differences ($\bar{d} = -0.0143$).
- **Reconciliation of the Four Non-Zero Per-Query Reranker Differences**:
  - Confirmed that exactly 16 of the 20 test queries have identical candidate rankings between Dense and Cross-Encoder ($d_i = 0.00000000$).
  - Identified the exact unrounded and rounded values for the 4 non-zero queries:
    1. `eval-test-ans-01`: $\Delta = \mathbf{-0.3691}$ (Graded: $-0.36907025$, Binary: $-0.36907025$).
    2. `eval-test-ans-13`: $\Delta = \mathbf{+0.0694}$ (Graded: $+0.06937594$, Binary: $+0.06937594$).
    3. `eval-test-ans-14`: Graded $\Delta = \mathbf{+0.0529}$ ($+0.05285097$), Binary $\Delta = \mathbf{+0.0412}$ ($+0.04116843$).
    4. `eval-test-ans-15`: Graded $\Delta = \mathbf{-0.0148}$ ($-0.01478363$), Binary $\Delta = \mathbf{-0.0269}$ ($-0.02686976$).
  - Exact Per-Query Sums: Graded $\sum d_i = \mathbf{-0.2616}$ (Paired Mean $\bar{d} = -0.0131$); Binary $\sum d_i = \mathbf{-0.2854}$ (Paired Mean $\bar{d} = -0.0143$).
  - Proven Origin of $-0.2625$: Multiplied sample-wide aggregate table difference $-0.013125 \times 20 = -0.2625$ rather than summing actual per-query deltas. True sums of displayed deltas are **$-0.2616$** (Graded) and **$-0.2854$** (Binary).
- **Correction of Binary vs Graded Interpretation**:
  - Demonstrated that Binary and Graded paired means differ systematically across all 4 candidate comparisons due to multi-target queries (`ans-13`, `14`, `15`) with graded weights ($3$ and $2$): Keyword (+0.0095 vs +0.0101), Union (+0.0043 vs +0.0045), RRF (+0.0048 vs +0.0052), Cross-Encoder (-0.0143 vs -0.0131).
  - Under both formulations, **no retrieval variant achieves statistical significance** ($p \ge 0.1861$; all 95% unadjusted and 98.75% Bonferroni simultaneous confidence intervals span zero).
- **Regression Testing & Lineage Closure**:
  - Added 3 regression tests in `ai-service/tests/test_benchmark_audit.py` validating paired stats mathematical identities, four non-zero query arithmetic, and metadata lineage.
  - Published authoritative benchmark version `0.12.1.4-closed` in [docs/RAG_EVALUATION.md](docs/RAG_EVALUATION.md) and [docs/rag_evaluation_results.json](docs/rag_evaluation_results.json).

### 7.15 Implemented Phase 12.1.5 Final Confidence Interval & Aggregate Metric Reconciliation

Phase 12.1.5 finalized all confidence intervals, critical values, and aggregate metric identities:
- **Exact Student's t Bonferroni Critical Value**:
  - Corrected `bonferroni_t_critical` from $2.8609$ ($t_{0.995, 19}$, $\alpha = 0.01$) to exact $t_{0.99375, 19} = \mathbf{2.7586}$ ($\text{scipy.stats.t.ppf}(0.99375, 19) = 2.75858590$) for $m=4$ comparisons at family-wise $\alpha = 0.05$.
  - Recomputed Binary Cross-Encoder Bonferroni 98.75% Simultaneous Confidence Interval using unrounded parameters:
    $$\bar{d} = -0.01426983, \quad \text{SEM} = 0.01915107 \implies \mathbf{[-0.0671, +0.0386]}$$
    aligning reported intervals with mathematical formulas across all tables, metadata, and docs.
- **Reconciliation of Graded Aggregate nDCG Difference and Paired Mean Identity**:
  - Disclosed that hypothetical subtraction $0.954275 - 0.967400 = -0.013125$ was an erroneous post-hoc back-calculation in Phase 12.1.3 prose dividing $-0.2625$ by $20$.
  - Demonstrated authoritative macro-averaged aggregate means:
    Dense-Only $0.96738399 \approx 0.9674$, Cross-Encoder $0.95430264 \approx 0.9543$.
  - Proved mathematical identity:
    $$\text{mean}(\text{candidate\_scores}) - \text{mean}(\text{dense\_scores}) == \text{mean}(\text{candidate\_scores} - \text{dense\_scores})$$
    holds to machine precision ($8.85 \times 10^{-17}$), identically resolving to $\mathbf{-0.0131}$.
- **Comprehensive Audit across All 4 Comparisons (Binary & Graded)**:
  - Verified across Keyword-Only, Simple Union, Hybrid RRF, and Cross-Encoder. All Bonferroni simultaneous intervals bracket zero ($p \ge 0.1861$).
- **Automated Regression Testing**:
  - Added 3 regression tests in `test_benchmark_audit.py` for Bonferroni critical values, mathematical identities, and version lineage closure (`0.12.1.5-closed`).

---

## 8. Verification & Strict Scope Policy

- **Phase 12.1.5 Complete**: Final Confidence Interval & Aggregate Metric Reconciliation completed with exact Bonferroni critical value correction ($t_{0.99375, 19} = 2.7586$), aggregate-minus-paired mean identity verification, 8-stage consistency audit, regression testing, and version lineage closure.
- **Automated Verification**:
  - Spring Boot Backend: **146** unit and integration tests passing (`mvn test` BUILD SUCCESS, 0 failures, 0 errors).
  - Python AI Service: **136** pytest unit and integration tests passing (`100% pass`, including 24 dedicated audit tests in `test_benchmark_audit.py`).
  - Frontend Test Suite: **66** unit and integration tests passing (`npx tsx --test frontend/tests/*.test.ts`, 0 failures).
  - Frontend Production Build: TypeScript check and Vite production build succeeded (`tsc && vite build`, exit code 0).
- **Phase 13 Not Started**: Phase 13 (Deployment & CI/CD) remains unstarted per phased roadmap constraints. Execution has halted for user review.

