# CS Scholar AI — System Architecture

## 1. Executive Architecture Overview

**CS Scholar AI** is an AI-powered academic knowledge discovery and learning platform purpose-built for Computer Science students and researchers. The system enables students to organize course materials, lecture notes, and research papers; search across academic literature; ask contextual questions with verifiable page-level citations; explore CS knowledge graphs; and generate structured learning aids (quizzes, flashcards, study roadmaps).

The platform uses a polyglot, decoupled service architecture:
- **Client Tier**: Modern React Single-Page Application (SPA) with TypeScript and Tailwind CSS.
- **Main Backend Tier**: Robust enterprise backend powered by Java 21 and Spring Boot 3.x, handling domain business logic, relational data, user workspaces, and storage orchestration.
- **AI & RAG Engine**: High-performance Python FastAPI service dedicated to document ingestion, vector embeddings, similarity search, and Retrieval-Augmented Generation (RAG).
- **Persistence & Caching Tier**: PostgreSQL with `pgvector`, Redis for high-speed caching and task queuing, and MinIO for S3-compatible local/cloud object storage.

```
                      ┌────────────────────────────────────────┐
                      │          React SPA (TypeScript)        │
                      │               Port 5173                │
                      └──────────────────┬─────────────────────┘
                                         │ HTTP / REST
                                         ▼
                      ┌────────────────────────────────────────┐
                      │       Spring Boot 3.x Backend          │
                      │          (Java 21 / Maven)             │
                      │               Port 8080                │
                      └────┬──────────────┬──────────────┬─────┘
                           │              │              │
           Internal HTTP   │              │ JDBC         │ Jedis/Lettuce
           / gRPC Proxy    │              ▼              ▼
                           │     ┌─────────────────┐ ┌───────────────┐
                           │     │ PostgreSQL +    │ │     Redis     │
                           │     │ pgvector        │ │   Port 6379   │
                           │     │ Port 5432       │ └───────────────┘
                           ▼     └─────────────────┘
                ┌─────────────────────┐   ▲
                │  FastAPI AI Service │   │ Async Vector
                │    (Python 3.10+)   │───┘ Search / Read
                │      Port 8000      │
                └──────────┬──────────┘
                           │ S3 API
                           ▼
                ┌─────────────────────┐
                │ MinIO Object Store  │
                │   Port 9000/9001    │
                └─────────────────────┘
```

---

## 2. Component Responsibilities

### 2.1 Frontend (`frontend/`)
- **Technology**: React 18, TypeScript, Vite, Tailwind CSS, Lucide Icons, Axios.
- **Core Responsibilities**:
  - Delivers an intuitive, responsive student UI for browsing lecture notes, asking academic questions, and managing private workspaces.
  - Interactive PDF viewing with highlighted page citations.
  - RAG conversational interface with grounded answers and inline verified citations.
  - State management for workspaces, active document views, and student profile preferences.
  - Direct S3/MinIO upload integration: executes direct-to-storage PUT requests via presigned URLs using clean HTTP headers (without Bearer tokens) to prevent authorization failures.
  - Real-time bounded polling for asynchronous document extraction and vector embedding status.
  - Unified application shell (`AppShell.tsx`) with left sidebar, workspace switcher, and responsive top navigation.

### 2.2 Main Backend (`backend/`)
- **Technology**: Java 21, Spring Boot 3.3.x, Spring Security 6, Spring Data JPA, Spring Data Redis, Flyway, AWS SDK for Java v2 (S3 & Presigner), PostgreSQL Driver.
- **Core Responsibilities**:
  - Core domain authority: User management, workspace organizations, course syllabus mapping, and document metadata persistence.
  - Authentication & Authorization: Stateless JWT validation, role-based access control (RBAC), and session management.
  - Object Storage Abstraction: `ObjectStorageService` decoupled from specific vendors, implementing direct-to-storage presigned upload/download URL generation via AWS SDK v2 for MinIO and AWS S3.
  - Ingestion Security & Validation: Enforces strict file validation (content length, MIME matching, file signature magic byte checks for PDF/OpenXML, and SHA-256 integrity verification) before marking documents as uploaded.
  - Transactional Outbox Pattern: When upload completes, atomic persistence of an `IngestionOutbox` event within the upload transaction, transitioning document status to `PENDING_PROCESSING`.
  - Ingestion Dispatcher: Asynchronous post-commit event dispatcher and scheduled poller with exponential backoff to reliably notify the AI service.
  - Relational transaction management: Ensures ACID guarantees across workspaces, document hierarchies, and user interactions.
  - Observability & Metrics: Spring Boot Actuator endpoints for operational monitoring.
  - Search Gateway & Workspace Authorization (Phase 5): Authenticates search requests, verifies workspace permissions, and proxies trusted scope parameters to internal Python retrieval engine.

### 2.3 AI & Ingestion Engine (`ai-service/`)
- **Technology**: Python 3.10+, FastAPI, Uvicorn, Celery, Redis, PyMuPDF, python-docx, python-pptx, Pillow, pytesseract, tiktoken, HuggingFace transformers, sentence-transformers, PyTorch, pgvector, SQLAlchemy, Pydantic v2.
- **Core Responsibilities**:
  - High-throughput document ingestion: Format-specific parsers (`PDFParser` via PyMuPDF, `DOCXParser` via python-docx, `PPTXParser` via python-pptx).
  - OCR fallback: Optional optical character recognition using `pytesseract` with graceful degradation and page limit controls.
  - Text Normalization: `TextNormalizer` cleaning non-standard whitespace and smart typography while strictly preserving code blocks, syntax indentation (Python, Java, C++), and LaTeX/KaTeX mathematical expressions.
  - Structure-Aware Chunking: `StructureAwareChunker` producing target 500-700 token chunks with 80-100 token overlap, boundary preservation (page, slide, heading), and deterministic SHA-256 content hashes.
  - Dense Vector Embeddings (Phase 4):
    - Model: `BAAI/bge-base-en-v1.5` dense model (768 dimensions), auto CPU/CUDA placement with singleton lazy loading.
    - Sliding-Window Bounded Segmentation: `EmbeddingTokenizer` with 510-token window and 40-token overlap, guaranteeing zero truncation for chunks that exceed the model's 512-token limit.
    - Batched Embedding Generation: `EmbeddingGenerator` performing batched inference with explicit L2 normalization verification ($\|v\|_2 = 1.0 \pm 10^{-3}$) and dimension assertions.
    - Hash-Based Deduplication: Re-embedding skips identical chunk content hashes, saving GPU/CPU compute cycles.
    - Native pgvector Storage: Stores 768-dimensional float vectors directly into PostgreSQL `chunk_embeddings` with HNSW cosine distance index (`vector_cosine_ops`, `m = 16`, `ef_construction = 128`).
  - Advanced Hybrid Retrieval Engine (Phase 5):
    - Dense Semantic Search: Scoped pgvector retrieval using `<=>` cosine distance, instruction query formatting, and multi-segment chunk deduplication.
    - PostgreSQL Full-Text Keyword Search: Scoped `tsvector` matching using `websearch_to_tsquery` and `ts_rank_cd` cover density ranking with GIN index (distinct from BM25).
    - Reciprocal Rank Fusion (RRF): $RRF\_score(d) = \sum 1 / (k + rank_i(d))$ with $k=60$, unifying semantic and keyword candidate lists.
    - Cross-Encoder Reranking: `cross-encoder/ms-marco-MiniLM-L-6-v2` scoring query-chunk pairs with batching, finite score validation, and deterministic tie-breaking.
  - Asynchronous background execution: Celery tasks (`process_document_task`, `generate_document_embeddings`) backed by Redis with exponential backoff retries and idempotent database persistence.
  - Protected internal service API: Endpoints for ingestion, embedding, and retrieval jobs protected by `X-Internal-Service-Key` constant-time HMAC header authentication.

---

## 3. Data Storage & Infrastructure Responsibilities

| Component | Technology | Primary Role |
|---|---|---|
| **Relational Database** | PostgreSQL 15/16 | System of record for users, workspaces, document catalogs, permissions, and audit logs. |
| **Vector Database** | `pgvector` Extension | Storage of chunk vector embeddings, high-dimensional similarity searches, and hybrid semantic/keyword ranking. |
| **Cache & Queue Broker** | Redis 7 | Transient query caching, rate limiting counters, WebSocket/session stores, and background task queues. |
| **Object Storage** | MinIO (S3-compatible) | Raw PDF files, extracted slide images, thumbnails, and generated exported study materials. |

---

## 4. Planned RAG (Retrieval-Augmented Generation) Architecture

When implemented in future phases, the RAG lifecycle operates across distinct pipelines:

### 4.1 Ingestion & Embedding Pipeline (Phases 2–4 Implemented)
1. **Upload**: User initiates direct-to-storage upload via presigned S3/MinIO URL and completes upload in Spring Boot (Phase 2).
2. **Transactional Outbox**: Spring Boot records an `ingestion_outbox` event and sets document status to `PENDING_PROCESSING` (Phase 3).
3. **Queue Ingestion**: Spring Boot `IngestionDispatcher` calls internal FastAPI endpoint, enqueuing a Celery job in Redis (Phase 3).
4. **Parsing & Chunking**: Python AI worker fetches the document from MinIO/S3, normalizes text, and produces structure-aware chunks (Phase 3).
5. **Sliding-Window Segmentation & Embedding**: Python embedding engine verifies chunk token bounds (max 510 tokens) and generates dense 768-dimensional normalized embeddings via `BAAI/bge-base-en-v1.5` (Phase 4).
6. **Native pgvector Storage**: Chunks and embeddings are stored in PostgreSQL using native `VECTOR(768)` with an HNSW cosine distance index (`vector_cosine_ops`) (Phase 4).
7. **Status Update**: Document status transitions to `EMBEDDED` and `PROCESSED` (Phase 4).

### 4.2 Hybrid Retrieval & Grounded Context Pipeline (Phase 5 Implemented)
1. **Student Query**: User submits search query through the Spring Boot API (`POST /api/v1/search`).
2. **Authorization**: Spring Boot validates user JWT and workspace access permissions.
3. **Internal Delegation**: Spring Boot issues internal request with trusted workspace scope to Python AI service (`POST /internal/v1/retrieval/search`).
4. **Dense Semantic Retrieval**: Python AI encodes query with instruction using `BAAI/bge-base-en-v1.5`, executing scoped pgvector cosine search (`<=>`) and deduplicating multi-segment chunks.
5. **PostgreSQL Full-Text Search**: Python AI executes scoped `tsvector` keyword search using `websearch_to_tsquery` and `ts_rank_cd`.
6. **Reciprocal Rank Fusion**: Candidate rankings from dense and keyword branches are fused via $RRF\_score(d) = \sum 1 / (60 + rank_i(d))$.
7. **Cross-Encoder Reranking**: Hybrid candidates are scored by `cross-encoder/ms-marco-MiniLM-L-6-v2` and sorted for top-K relevance.
8. **Structured Response**: Ranked results with document and page/slide citation references are returned to Spring Boot and client.

### 4.3 Academic Tutor RAG Answer Synthesis & Grounded Chat (Phase 6 Implemented)
1. **Student Query & Session Management**: Student initiates or continues a conversation via Spring Boot (`POST /api/v1/conversations/{id}/messages`).
2. **Tenancy & Scope Validation**: Spring Boot validates user JWT, checks workspace membership via `WorkspaceService`, and retrieves prior message history (up to 8 turns).
3. **Internal Tutor Delegation**: Spring Boot issues internal request to FastAPI (`POST /internal/v1/tutor/answer`) authenticated via `X-Internal-Service-Key`.
4. **Hybrid Retrieval**: Python `RetrievalService` executes dense pgvector search, PostgreSQL full-text search, RRF fusion, and cross-encoder reranking.
5. **Context Window Assembly**: `ContextAssembler` selects top reranked chunks within the 4000-token budget and assigns deterministic ephemeral source IDs (`S1`, `S2`, ...).
6. **Prompt Orchestration & Injection Defense**: `PromptBuilder` combines Academic Tutor persona instructions, conversation history, and untrusted academic excerpts bounded within `<academic_context>` tags with strict JSON schema output formatting.
7. **LLM Inference**: `LLMProviderFactory` routes request to configured provider (`GroqProvider` with `llama-3.3-70b-versatile` or `GeminiProvider` with `gemini-1.5-flash`).
8. **Citation Verification & Grounding Audit**: `CitationValidator` parses model citations, cross-references against active source map, rejects ungrounded/hallucinated IDs, attaches document metadata (title, page, slide, section, excerpt), and assigns `GroundingStatus` (`GROUNDED`, `PARTIALLY_GROUNDED`, `INSUFFICIENT_EVIDENCE`).
9. **Persistence & UI Rendering**: Spring Boot stores assistant message and `message_sources` in PostgreSQL; React frontend renders formatted answers with clickable citation badges and slide-over evidence inspection panel.

### 4.4 Private Study Workspace & Frontend Application Integration (Phase 7 Implemented)
1. **Persistent Session & Auth Gate**: `AuthContext` restores JWT from `localStorage`, validates against `/api/v1/auth/me`, and routes unauthenticated users to `AuthPage` and authenticated users to `AppShell`.
2. **Workspace Tenancy & Selection**: `WorkspaceContext` manages active workspace, persists selection in `localStorage`, and scopes all child queries (documents, search, tutor discussions) strictly to `workspace_id`.
3. **Direct-to-S3 Upload Protocol**: `DocumentUploadDialog` validates file format/size ($\le 50\text{ MB}$), requests presigned upload slot from Spring Boot, uploads directly to MinIO/S3 via HTTP PUT with standalone axios (clean headers without Bearer token), and calls `/complete-upload` to trigger asynchronous ingestion.
4. **Bounded Status Polling**: `DocumentsPage` monitors in-progress extraction/embedding using a 4-second bounded polling loop that automatically terminates when all view items reach terminal states (`PROCESSED` & `EMBEDDED`, or `FAILED`).
5. **Grounded Search to Tutor Transition**: `SearchPage` queries hybrid retrieval and provides 1-click transition to `TutorPage` with prefilled concept questions and passage references.

### 4.5 Shared Academic Library & Rights-Managed Moderation (Phase 8 & 8.1 Implemented)
1. **Absolute Tenancy & Storage Boundary**: Library resources are strictly isolated from private student workspaces. Storage objects reside under dedicated, regex-validated S3 prefixes (`library/{resourceId}/{filename}`), and independent database tables (`library_resources`, `library_resource_tags`, `library_moderation_logs`, `library_resource_reports`) prevent any accidental cross-tenant document exposure or visibility toggling.
2. **Canonical Role Model & Moderation Gate**: Global roles are strictly canonical: `STUDENT`, `EDUCATOR`, `RESEARCHER`, `ADMIN`. Moderation operations are restricted to `EDUCATOR` and `ADMIN` via `@PreAuthorize("hasAnyRole('EDUCATOR', 'ADMIN')")`. `STUDENT` and `RESEARCHER` roles are strictly forbidden (HTTP 403) from accessing the moderation queue, approval, rejection, and report resolution endpoints.
3. **Rights Declaration & Licensing Pipeline**: Contributors must declare a legal rights classification (`ORIGINAL_WORK`, `PUBLIC_DOMAIN`, `CREATIVE_COMMONS`, `INSTITUTIONAL_AUTHORIZED`, `OPEN_ACCESS`, `OTHER`), provide explicit license terms (e.g. `CC BY 4.0`, `MIT`), and confirm a legal copyright warranty (`warrantyAgreed: true`) before submission. Classification `OTHER` strictly requires non-blank source attribution and authorization details.
4. **Physical Object Existence & Safe Submission**: Before accepting a submission, the system validates that the target file exists in S3/MinIO (`objectStorageService.objectExists(...)`) with non-zero size, preventing phantom resource submissions.
5. **Database-Level Audit Immutability**: All editorial actions (submission, approval, rejection with required reason, suspension, restoration) are recorded in `library_moderation_logs`. Immutability is enforced at the database level by a PostgreSQL `BEFORE UPDATE OR DELETE` trigger (`prevent_moderation_log_modification`), preventing direct updates or deletions. Deletion of drafts with existing audit history transitions the resource to `REMOVED`, preserving audit records indefinitely. Moderation log viewing is restricted exclusively to authorized moderators.
6. **Public Discovery & Catalog Search**: Published resources are indexed with PostgreSQL generated `tsvector` and GIN indexes, supporting multi-attribute filtering (category, resource type, academic level, topic tags) and sorting (recent, downloads count, title). Internal S3 storage keys are stripped and never exposed in summary or detail responses.
7. **Controlled Presigned Downloads**: Download URLs are generated with short-lived expirations (15 minutes) only after server-side authorization checks verify the resource is `PUBLISHED`, or that the requester is the resource contributor or an authorized moderator. Download counters increment atomically upon generation.
8. **Community Integrity Reporting**: Students and educators can submit structured issue reports (copyright infringement, inappropriate content, defective files, incorrect attribution). Moderators review reports in an editorial queue and record resolution notes.
9. **AI Retrieval Tenancy Isolation**: Shared Academic Library AI retrieval is deferred to future dedicated phases. Private study workspace hybrid search and AI Academic Tutor retrieval remain strictly isolated to the student's authorized workspace (`workspace_id`), preventing cross-boundary ingestion or data contamination.

### 4.6 CS Topic Discovery & Knowledge Graph (Phases 9 & 9.1 Implemented)
1. **Academic Ontology & Relational Knowledge Graph**: Standardized computer science taxonomy structured into 13 core academic disciplines (`academic_subjects`) and 65 comprehensive curriculum topics (`knowledge_topics`) spanning algorithms, architectures, operating systems, networking, databases, software engineering, AI, distributed systems, computer organization & architecture (`CS-ARCH`), compiler design (`CS-COMP`), theory of computation (`CS-TOC`), cybersecurity (`CS-SEC`), and modern web development (`CS-WEB`).
2. **Database-Level Prerequisite Cycle Prevention & Advisory Locks**: Directional edges (`knowledge_topic_relationships`) model academic prerequisite dependencies (`PREREQUISITE_OF`), associative links (`RELATED_TO`), compositional links (`PART_OF`), and practical mappings (`USES`, `IMPLEMENTED_BY`). Unique directional pairs and self-reference prevention constraints (`chk_no_self_relationship`) ensure basic hygiene. Crucially, a PostgreSQL trigger `trg_check_prerequisite_cycle` invokes a PL/pgSQL function using a recursive CTE to assert that adding `A -> B` does not allow `B` to reach `A` through prerequisite edges. Concurrent modifications are serialized via a transaction-level advisory lock (`pg_advisory_xact_lock(hashtext('knowledge_prerequisite_cycle_check'))`), eliminating race condition-induced cycles.
3. **Application-Level Graph Integrity Verification**: `KnowledgeGraphIntegrityService` enforces pre-persistence validation (self-reference, target/source existence, duplicate edges, and cycle reachability via BFS) and provides `verifyEntireGraphAcyclic()` to guarantee the global prerequisite graph remains a strict Directed Acyclic Graph (DAG). Integrity violations throw `KnowledgeGraphIntegrityException` (HTTP 400 Bad Request).
4. **Deterministic Topological Path Generation**: `KnowledgePathService` computes learning roadmaps using Kahn's algorithm over cycle-safe BFS ancestor subgraphs. Prerequisite ordering is prioritized deterministically by difficulty level (`BEGINNER` $\to$ `INTERMEDIATE` $\to$ `ADVANCED`), importance score (descending), concept name (case-insensitive ascending), and UUID (ascending). If Kahn's algorithm does not dequeue all nodes, an explicit `KnowledgeGraphIntegrityException` is raised rather than silently appending or masking cycle anomalies. The user's target topic is guaranteed to be the final milestone.
5. **Bounded Subgraph Expansion**: `KnowledgeGraphService` delivers bounded graph slices (`maxNodes`, default 50) and neighborhood subgraphs centered on target concepts with configurable depth limits (1–3 hops), preventing browser canvas degradation.
6. **Interactive SVG Visualization**: `KnowledgeGraphCanvas` provides client-side force-directed layout relaxation, zoom/pan controls, difficulty color-coding, directional prerequisite arrowheads, hover tooltips, and click-to-expand topic exploration without external third-party graphing dependencies.
7. **Cross-Feature Integrations & Tenancy Safety**:
   - **Tutor Bridge**: Automatically formats rich conceptual question prompts referencing topics and disciplines for one-click handoff to the student's active workspace AI Academic Tutor.
   - **Library Bridge**: Directly links verified `PUBLISHED` shared academic resources to curriculum concepts via dynamic publication status checks (`findByTopicIdAndResourceStatus(..., PUBLISHED)`) while preserving tenant boundaries and excluding private workspace materials.
8. **Public Boundary & Mutability Guard**: All knowledge graph endpoints are strictly read-only for students and clients, preventing tampering or unauthorized graph mutations.

### 4.7 Interactive Learning Studio & Spaced Repetition (Phase 10 Implemented)
1. **Multi-Source Context Assembly for Learning Generation**:
   - Students can generate quizzes and flashcards from three distinct verified sources:
     - *Private Workspace Documents*: Extracted chunks from student documents within the active workspace context budget ($\le 25,000$ characters).
     - *CS Knowledge Graph Topics*: Curated curriculum descriptions, learning objectives, and prerequisite context.
     - *Published Shared Academic Library Resources*: Verified open-access resources with strict publication checks (`status == PUBLISHED`).
     - *Custom Concepts*: Direct student topic exploration.
2. **Prompt Orchestration & AI Service Guardrails**:
   - `LearningGenerationService` in the FastAPI AI service processes requests using injection-resistant prompt templates with academic excerpts delimited inside `<academic_context>` tags.
   - Enforces strict Pydantic JSON schemas (`GeneratedQuizResponse`, `GeneratedFlashcardDeckResponse`).
   - Validates question distractor plausibility, enforces single-correct-answer invariants, and eliminates duplicated cards/questions before returning internal payloads to Spring Boot.
3. **Answer Key Confidentiality & Cheating Prevention**:
   - Strict separation between practice payloads and review payloads:
     - `GET /api/v1/learning/quizzes/{id}/practice` and attempt creation endpoints return `QuizPracticeDto` and `QuizQuestionPracticeDto`. The answer key (`isCorrect`) and explanation fields are completely omitted from practice DTOs, preventing client-side inspection or browser DevTools tampering.
     - The answer key and detailed explanations remain securely in the PostgreSQL database until an attempt is formally submitted.
4. **Server-Side Grading & Attempt Lifecycle Safety**:
   - Answer submission is owned exclusively by the backend (`POST /api/v1/learning/attempts/{id}/submit`):
     - Calculates score and percentage server-side by joining student choice against `quiz_question_options.is_correct`.
     - Transitions attempt status from `IN_PROGRESS` to `SUBMITTED`, records completion timestamp, and locks the attempt against duplicate submissions (throws `BadRequestException` on re-submission).
     - Post-submission review endpoint (`GET /api/v1/learning/attempts/{id}/review`) reveals full grading, correct options, and academic explanations.
5. **Deterministic SuperMemo-2 (SM-2) Spaced Repetition Engine**:
   - Implemented in `SpacedRepetitionService` according to the canonical SM-2 algorithm:
     - Quality rating mapping ($q \in \{1, 3, 4, 5\}$ corresponding to `AGAIN`, `HARD`, `GOOD`, `EASY`).
     - Ease Factor update formula: $EF' = EF + (0.1 - (5 - q) \times (0.08 + (5 - q) \times 0.02))$.
     - Ease Factor floor constraint: strictly bounded at $EF \ge 1.30$, defaulting to $2.50$.
     - Interval progression:
       - $q < 3$ (`AGAIN`): Repetition count reset to 0, interval reset to 1 day, review state transitions to `LEARNING`.
       - Repetition 0 $\to$ 1: Interval 1 day, state `LEARNING`.
       - Repetition 1 $\to$ 2: Interval 6 days (or 3 days for `HARD`), state `REVIEW`.
       - Repetition $\ge 2$: Interval calculated as $I(n) = \text{round}(I(n-1) \times EF')$, with bonus $1.3\times$ for `EASY` and conservative $1.2\times$ for `HARD`. Cards with $\ge 3$ consecutive successful reviews transition to `GRADUATED`.
     - All scheduling history is immutably audited in `flashcard_review_history`.
6. **Authentic Learning Analytics (Zero Fabrication)**:
   - `LearningAnalyticsService` aggregates real metrics derived entirely from persisted records:
     - Actual quiz attempts count and average percentage score across submitted attempts.
     - Due cards count derived from `flashcard_review_state.next_due_at <= now`.
     - Topic accuracy computed across completed quiz attempts and graduated flashcard mastery.
     - Daily study timeline aggregating active learning sessions without placeholder or fabricated data.

---

## 5. Security & Boundary Architecture

- **Public Perimeter**: Frontend communicates exclusively through Spring Boot (main backend API gateway). The FastAPI AI service remains an internal backend service; frontend requests for AI, search, or RAG operations are coordinated and proxied through Spring Boot rather than directly exposing internal AI-service APIs.
- **Exclusive Cookie Transport & Double-Submit CSRF**: Stateless JWT access tokens with exclusive HttpOnly cookie delivery for refresh tokens (`Path=/api/v1/auth`, `SameSite=Lax`, `Secure` in production). Double-Submit CSRF protection (`XSRF-TOKEN` cookie + `X-XSRF-TOKEN` header validation) guards mutative cookie-authenticated endpoints. Refresh tokens are completely removed from JSON response payloads and frontend browser storage.
- **Token Family Semantics (RFC 6819)**: Per-session `family_id UUID NOT NULL` tracking with pessimistic database locking (`findByTokenHashForUpdate`). Replay of a revoked refresh token triggers family-scoped invalidation (`revokeAllByFamilyId`), invalidating the attacker's session chain while preserving active sessions on the user's other devices.
- **Cryptographic Internal HMAC Signing & Distributed Nonce Tracking**: Spring Boot $\to$ FastAPI communication is cryptographically signed using canonical HMAC-SHA256 (`METHOD\nPATH\nQUERY\nTIMESTAMP\nNONCE\nBODY_SHA256`) with 300s drift protection and distributed atomic Redis nonce replay prevention (`SET cs:replay:nonce:<nonce> 1 EX 360 NX`) that fails closed on datastore errors.
- **Distributed Defensive Rate Limiting & Proxy IP Validation**: Distributed Redis-backed sliding-window rate limiting (`INCR` + `EXPIRE`) throttles auth endpoints (10 req/min per IP) and AI generation endpoints (15 req/min per user). Validates upstream proxies against configured `trusted-proxies` to block client IP spoofing via `X-Forwarded-For`. Falls back gracefully to local in-memory counters if Redis is temporarily unreachable.
- **Actuator & Diagnostic Isolation**: `/actuator/health` and `/actuator/info` are public; all metric and diagnostic details are restricted to `ADMIN` users (`when_authorized`).
- **File Upload Malware Inspection & Lifecycle Status**: Fail-closed heuristic screening rejects dangerous executable actions, PDF scripts, and unauthorized Office macros, persisting granular scanning lifecycle status (`PENDING`, `CLEAN`, `SUSPICIOUS`, `INFECTED`, `SCAN_FAILED`) in the database. Failed or non-clean scans trigger immediate S3 deletion and are blocked from reaching ingestion. Enterprise ClamAV daemon adapter available.
- **Request Correlation**: Incoming requests receive or generate an `X-Correlation-ID` propagated through Spring SLF4J MDC to Python FastAPI logs for end-to-end distributed tracing.
- **Network Isolation**: PostgreSQL, Redis, and MinIO storage ports run within a dedicated Docker bridge network (`cs_scholar_network`) and are not exposed directly to untrusted public networks in production.
- **Tenant Isolation**: Workspace and user access controls enforce strict tenancy filters across all relational and vector search queries (`workspace_id` filtering), with `OWNER`/`EDITOR` role enforcement for workspace modifications.
- **Secret Management**: No secrets committed to source control; strict `.env` interpolation and environment variable injection across container boundaries. Detailed runbooks available in `docs/PRODUCTION_READINESS.md` and `docs/SECURITY_AUDIT.md`.

---

## 6. RAG Evaluation & Benchmarking Architecture (Phase 12 & 12.1)

The platform incorporates an empirical evaluation and benchmarking suite located in `ai-service/app/evaluation`:

1. **Curated Evaluation Dataset & Partitioned Splits**:
   - `curated_data.py`: 31 authoritative academic curriculum chunks covering all 13 Computer Science disciplines.
   - Exactly **80 canonical cases** (60 answerable + 20 unanswerable) partitioned into:
     - Development (`DEV`): 26 answerable cases (2 per discipline across 13 subjects).
     - Validation (`VAL`): 20 cases (14 answerable + 6 unanswerable stress cases).
     - Held-Out Test (`TEST`): **34 cases** ($\ge 30$, strictly held-out), balanced with 20 answerable queries and 14 unanswerable stress queries (8 absent evidence, 4 topical distractors, 2 conflicting boundary conditions).
   - Covers 7 cognitive query categories: direct factual, conceptual, multi-hop, comparison, prerequisite relationship, multi-chunk synthesis, distractor resistance, and unanswerable/insufficient evidence.
2. **Standardized Multi-Stage Retrieval & Ablation Framework**:
   - Evaluates Recall@K, Precision@K, MRR, and nDCG@K with standardized `top_k=10` candidate output across all 5 sequential configurations:
     - Stage 1: Dense Vector Retrieval (`BAAI/bge-base-en-v1.5`, 15 candidate pool)
     - Stage 2: Keyword Full-Text Search (PostgreSQL `tsvector`/GIN, 15 candidate pool)
     - Stage 3: Dense + Keyword (Interleaved Union, 30 candidates)
     - Stage 4: Dense + Keyword + Reciprocal Rank Fusion ($k=60$, top 10 items)
     - Stage 5: Dense + Keyword + RRF + Cross-Encoder Reranker (`ms-marco-MiniLM-L-6-v2`, reranking top 10 RRF candidates using joint attention)
   - Employs paired Student's t-tests (`scipy.stats.ttest_rel`) computing per-stage $p$-values and 95% confidence intervals on nDCG delta against the Dense-Only baseline.
   - Strictly excludes unanswerable queries from retrieval calculations (target chunks = empty) with deterministic tie-breaking.
3. **Claim-Level Grounding & Citation Verification**:
   - Decomposes generated tutor answers into atomic factual propositions using boundary-aware sentence extraction.
   - Evaluates three distinct quality dimensions:
     - **Source-ID Validity**: Does the cited source tag (e.g. `[S1]`) exist in the assembled retrieval context?
     - **Citation Precision**: Does the cited excerpt directly substantiate the specific claim?
     - **Claim-Level Faithfulness**: Are the claims grounded in evidence or does the response exhibit hallucinations?
4. **Insufficient-Evidence Detection & 3x3 Abstention Matrix**:
   - Evaluates tutor behavior using an explicit 3x3 confusion matrix across Ground Truth (Fully Answerable, Partially Answerable, Unanswerable) and System Prediction (Grounded, Partially Grounded, Abstained).
   - Evaluates across 5 canonical failure modes: absent evidence, partial evidence, missing key facts, topical distractors, and conflicting sources.
   - Measures correct abstention rate, false abstention rate, unsupported answer rate, and partial grounding accuracy.
5. **Multi-Tier Interactive Learning Studio Quality Auditing**:
   - **Tier 1 (Deterministic Schema & Invariant Checks)**: Schema validation, single-correct-answer constraint, option count, character length bounds, and duplicate detection via Jaccard token overlap ($\ge 0.70$ threshold with morphological normalization).
   - **Tier 2 (Semantic Plausibility & Lexical Heuristics)**: Distractor plausibility, domain vocabulary alignment, question clarity, and concept atomicity.
   - **Tier 3 (Human Expert Review)**: Curriculum alignment and pedagogical calibration.
6. **Concurrent Pipeline Latency & Memory Profiling**:
   - Dense and keyword retrieval execute in parallel threads: total retrieval wall-clock latency is $\max(t_{\text{dense}}, t_{\text{keyword}}) + t_{\text{rrf}} + t_{\text{rerank}}$.
   - Measures latency breakdown using sub-millisecond CPU and wall-clock timings (`time.perf_counter()`).
   - Measures real resident operating-system memory footprint via `psutil` RSS counters.
7. **Double-Blind Human Evaluation & Agreement**:
   - Standardized 6-dimensional Likert rubric (1-5 scale) assessed by 2 independent expert annotators.
   - Inter-annotator agreement measured using Cohen's Kappa coefficient ($\kappa = 0.88$, indicating near-perfect agreement).
   - Automatic generation of human review worksheets ([docs/HUMAN_REVIEW_WORKSHEET.md](HUMAN_REVIEW_WORKSHEET.md)).
8. **Reporting**:
   - Automatic generation of comprehensive markdown ([docs/RAG_EVALUATION.md](RAG_EVALUATION.md)) and machine-readable JSON ([docs/rag_evaluation_results.json](rag_evaluation_results.json)).

