# CS Scholar AI — Database Schema & Entity Relationships

## 1. Overview

CS Scholar AI utilizes **PostgreSQL** paired with the **`pgvector`** extension to unify transactional relational data with high-dimensional vector embeddings within a single database engine.

- **Phase 0**: Database baseline established with `vector` extension and Flyway baseline `V1__init_empty.sql`.
- **Phase 1 (Implemented)**: Core authentication, user identities, workspaces, workspace membership, and refresh token security created via `V2__create_users_workspaces_auth.sql`.
- **Phase 2 (Implemented)**: Document management catalog and S3-compatible object storage schema created via `V3__create_documents.sql`.
- **Phase 3 (Implemented)**: Document text chunks, asynchronous processing jobs, transactional outbox, and workspace role reconciliation via `V4__reconcile_roles_and_create_ingestion_tables.sql`.
- **Phase 4 (Implemented)**: Dense chunk embeddings (`pgvector VECTOR(768)`), HNSW vector index, embedding jobs, and document embedding status via `V5__create_chunk_embeddings.sql`.
- **Phase 5 (Implemented)**: PostgreSQL full-text search `tsvector` generated column and GIN index for hybrid retrieval via `V6__add_full_text_search_to_chunks.sql`.
- **Phases 6+ (Upcoming)**: AI Academic Tutor, RAG chat sessions, citation grounding, knowledge taxonomy, and learning modules.

---

## 2. Implemented Schema (Phases 1–5)

```mermaid
erDiagram
    USERS ||--o{ WORKSPACES : owns
    USERS ||--o{ WORKSPACE_MEMBERS : belongs_to
    WORKSPACES ||--o{ WORKSPACE_MEMBERS : contains
    USERS ||--o{ REFRESH_TOKENS : possesses
    USERS ||--o{ DOCUMENTS : owns
    WORKSPACES ||--o{ DOCUMENTS : contains
    DOCUMENTS ||--o{ DOCUMENT_CHUNKS : has
    DOCUMENT_CHUNKS ||--o{ CHUNK_EMBEDDINGS : has
    DOCUMENTS ||--o{ CHUNK_EMBEDDINGS : references
    DOCUMENTS ||--o{ PROCESSING_JOBS : tracks
    DOCUMENTS ||--o{ EMBEDDING_JOBS : tracks
    DOCUMENTS ||--o{ INGESTION_OUTBOX : dispatches

    USERS {
        uuid id PK
        varchar full_name
        varchar email UK
        varchar password_hash
        varchar role
        varchar profile_image_url
        boolean is_active
        timestamptz created_at
        timestamptz updated_at
    }

    WORKSPACES {
        uuid id PK
        varchar name
        text description
        uuid owner_id FK
        timestamptz created_at
        timestamptz updated_at
    }

    WORKSPACE_MEMBERS {
        uuid id PK
        uuid workspace_id FK
        uuid user_id FK
        varchar member_role
        timestamptz joined_at
    }

    REFRESH_TOKENS {
        uuid id PK
        uuid user_id FK
        varchar token_hash UK
        timestamptz expires_at
        boolean revoked
        timestamptz created_at
    }

    DOCUMENTS {
        uuid id PK
        uuid owner_id FK
        uuid workspace_id FK
        varchar title
        varchar original_filename
        varchar storage_key UK
        varchar file_type
        varchar mime_type
        bigint file_size
        varchar checksum_sha256
        int page_count
        text description
        varchar processing_status
        varchar embedding_status
        varchar visibility
        timestamptz created_at
        timestamptz updated_at
        timestamptz uploaded_at
        timestamptz deleted_at
    }

    DOCUMENT_CHUNKS {
        uuid id PK
        uuid document_id FK
        int chunk_index
        int processing_version
        int page_number
        int slide_number
        varchar section_title
        text content
        int token_count
        varchar source_type
        varchar extraction_method
        varchar content_hash
        jsonb metadata
        tsvector content_tsv
        timestamptz created_at
    }

    CHUNK_EMBEDDINGS {
        uuid id PK
        uuid chunk_id FK
        uuid document_id FK
        int segment_index
        vector embedding
        varchar model_name
        varchar model_version
        varchar content_hash
        int token_count
        timestamptz created_at
    }

    EMBEDDING_JOBS {
        uuid id PK
        uuid document_id FK
        varchar status
        varchar model_name
        varchar model_version
        int total_chunks
        int processed_chunks
        int total_segments
        int attempts
        int max_attempts
        text error_message
        varchar task_id
        timestamptz started_at
        timestamptz completed_at
        timestamptz created_at
        timestamptz updated_at
    }
        jsonb metadata
        timestamptz created_at
    }

    PROCESSING_JOBS {
        uuid id PK
        uuid document_id FK
        varchar status
        int attempts
        int max_attempts
        text error_message
        varchar task_id
        int processing_version
        timestamptz started_at
        timestamptz completed_at
        timestamptz created_at
        timestamptz updated_at
    }

    INGESTION_OUTBOX {
        uuid id PK
        uuid document_id FK
        varchar event_type
        jsonb payload
        varchar status
        int retry_count
        text last_error
        timestamptz created_at
        timestamptz dispatched_at
    }
```

### Table Definitions

#### `users`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique user identifier |
| `full_name` | VARCHAR(150) | NOT NULL | User's full display name |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | Normalized email address |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt password hash |
| `role` | VARCHAR(30) | NOT NULL, DEFAULT `'STUDENT'` | Authorization role (`STUDENT`, `INSTRUCTOR`, `ADMIN`) |
| `profile_image_url` | VARCHAR(500) | NULL | URL to user's profile avatar |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Account activation flag |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Account creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Last profile update timestamp |

**Indexes**:
- `idx_users_email` ON `users(email)`

#### `workspaces`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique workspace identifier |
| `name` | VARCHAR(100) | NOT NULL | Workspace title |
| `description` | TEXT | NULL | Workspace details / course description |
| `owner_id` | UUID | NOT NULL, FK -> `users(id)` ON DELETE CASCADE | Workspace creator / owner |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Last modification timestamp |

**Indexes**:
- `idx_workspaces_owner` ON `workspaces(owner_id)`

#### `workspace_members`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Membership record identifier |
| `workspace_id` | UUID | NOT NULL, FK -> `workspaces(id)` ON DELETE CASCADE | Associated workspace |
| `user_id` | UUID | NOT NULL, FK -> `users(id)` ON DELETE CASCADE | Member user |
| `member_role` | VARCHAR(30) | NOT NULL, DEFAULT `'MEMBER'` | Membership role (`OWNER`, `MEMBER`, `VIEWER`) |
| `joined_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Join timestamp |

**Constraints & Indexes**:
- `uq_workspace_member`: UNIQUE(`workspace_id`, `user_id`)
- `idx_workspace_members_workspace` ON `workspace_members(workspace_id)`
- `idx_workspace_members_user` ON `workspace_members(user_id)`

#### `refresh_tokens`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Token record identifier |
| `user_id` | UUID | NOT NULL, FK -> `users(id)` ON DELETE CASCADE | Associated user |
| `token_hash` | VARCHAR(255) | NOT NULL, UNIQUE | SHA-256 hash of refresh token |
| `expires_at` | TIMESTAMPTZ | NOT NULL | Token expiration timestamp |
| `revoked` | BOOLEAN | NOT NULL, DEFAULT FALSE | Revocation flag for rotation/logout |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Issuance timestamp |

**Indexes**:
- `idx_refresh_tokens_hash` ON `refresh_tokens(token_hash)`
- `idx_refresh_tokens_user` ON `refresh_tokens(user_id)`

#### `documents`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique document identifier |
| `owner_id` | UUID | NOT NULL, FK -> `users(id)` ON DELETE CASCADE | Document uploader/owner |
| `workspace_id` | UUID | NOT NULL, FK -> `workspaces(id)` ON DELETE CASCADE | Workspace container |
| `title` | VARCHAR(255) | NOT NULL | Document display title |
| `original_filename` | VARCHAR(255) | NOT NULL | Sanitized user filename |
| `storage_key` | VARCHAR(512) | NOT NULL, UNIQUE | S3 / MinIO object key |
| `file_type` | VARCHAR(50) | NOT NULL | Enum: `PDF`, `DOCX`, `PPTX` |
| `mime_type` | VARCHAR(100) | NOT NULL | Validated MIME type |
| `file_size` | BIGINT | NOT NULL | Verified file size in bytes |
| `checksum_sha256` | VARCHAR(64) | NULL | Hex-encoded SHA-256 hash |
| `page_count` | INTEGER | NULL | Extracted page count (Phase 3) |
| `description` | TEXT | NULL | Optional user document description |
| `processing_status` | VARCHAR(50) | NOT NULL, DEFAULT `'PENDING_UPLOAD'` | Status: `PENDING_UPLOAD`, `UPLOADED`, `PENDING_PROCESSING`, `PROCESSING`, `PROCESSED`, `FAILED`, `DELETING`, `DELETED` |
| `embedding_status` | VARCHAR(50) | NOT NULL, DEFAULT `'NOT_EMBEDDED'` | Status: `NOT_EMBEDDED`, `PENDING_EMBEDDING`, `EMBEDDING_IN_PROGRESS`, `EMBEDDED`, `FAILED` |
| `visibility` | VARCHAR(50) | NOT NULL, DEFAULT `'PRIVATE'` | Access level (default `PRIVATE`) |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Last updated timestamp |
| `uploaded_at` | TIMESTAMPTZ | NULL | Timestamp when S3 upload was verified |
| `deleted_at` | TIMESTAMPTZ | NULL | Soft deletion timestamp |

**Constraints & Indexes**:
- `uq_documents_storage_key`: UNIQUE(`storage_key`)
- `idx_documents_workspace_id` ON `documents(workspace_id)`
- `idx_documents_owner_id` ON `documents(owner_id)`
- `idx_documents_storage_key` ON `documents(storage_key)`
- `idx_documents_processing_status` ON `documents(processing_status)`
- `idx_documents_embedding_status` ON `documents(embedding_status)`
- `idx_documents_deleted_at` ON `documents(deleted_at)`
- `idx_documents_workspace_deleted` ON `documents(workspace_id, deleted_at)`

#### `document_chunks`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique chunk identifier |
| `document_id` | UUID | NOT NULL, FK -> `documents(id)` ON DELETE CASCADE | Parent document reference |
| `chunk_index` | INTEGER | NOT NULL | Sequential 0-based chunk index |
| `processing_version` | INTEGER | NOT NULL, DEFAULT 1 | Ingestion processing schema version |
| `page_number` | INTEGER | NULL | Extracted 1-based page number (PDF) |
| `slide_number` | INTEGER | NULL | Extracted 1-based slide number (PPTX) |
| `section_title` | VARCHAR(255) | NULL | Enclosing section, heading, or slide title |
| `content` | TEXT | NOT NULL | Normalized text content of the chunk |
| `token_count` | INTEGER | NOT NULL | Token count computed via `cl100k_base` |
| `source_type` | VARCHAR(50) | NOT NULL | `PDF`, `DOCX`, `PPTX` |
| `extraction_method` | VARCHAR(50) | NOT NULL | `DIRECT_TEXT`, `OCR`, `HYBRID` |
| `content_hash` | VARCHAR(64) | NOT NULL | SHA-256 hash of chunk content |
| `metadata` | JSONB | NOT NULL, DEFAULT `'{}'::jsonb` | Additional structural & provenance metadata |
| `content_tsv` | TSVECTOR | NOT NULL, GENERATED ALWAYS AS (to_tsvector('english', content)) STORED | Full-text search vector for English keyword indexing |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Chunk generation timestamp |

**Constraints & Indexes**:
- `uq_document_chunk`: UNIQUE(`document_id`, `processing_version`, `chunk_index`)
- `idx_document_chunks_document_id` ON `document_chunks(document_id)`
- `idx_document_chunks_version` ON `document_chunks(document_id, processing_version)`
- `idx_document_chunks_page` ON `document_chunks(document_id, page_number)`
- `idx_document_chunks_hash` ON `document_chunks(content_hash)`
- `idx_document_chunks_content_tsv` ON `document_chunks USING gin(content_tsv)`

#### `processing_jobs`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique job identifier |
| `document_id` | UUID | NOT NULL, FK -> `documents(id)` ON DELETE CASCADE | Target document reference |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT `'QUEUED'` | Job lifecycle status (`QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED`) |
| `attempts` | INTEGER | NOT NULL, DEFAULT 0 | Current execution attempt count |
| `max_attempts` | INTEGER | NOT NULL, DEFAULT 3 | Maximum allowed retry attempts |
| `error_message` | TEXT | NULL | Captured exception or failure trace |
| `task_id` | VARCHAR(255) | NULL | Background Celery asynchronous task ID |
| `processing_version` | INTEGER | NOT NULL, DEFAULT 1 | Ingestion version |
| `started_at` | TIMESTAMPTZ | NULL | Processing initiation timestamp |
| `completed_at` | TIMESTAMPTZ | NULL | Processing completion timestamp |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Job creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Last status update timestamp |

**Indexes**:
- `idx_processing_jobs_document_id` ON `processing_jobs(document_id)`
- `idx_processing_jobs_status` ON `processing_jobs(status)`

#### `ingestion_outbox`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique outbox event identifier |
| `document_id` | UUID | NOT NULL, FK -> `documents(id)` ON DELETE CASCADE | Associated document |
| `event_type` | VARCHAR(50) | NOT NULL, DEFAULT `'INGESTION_REQUESTED'` | Outbox event type |
| `payload` | JSONB | NOT NULL | Serialized dispatch payload |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT `'PENDING'` | Dispatch status (`PENDING`, `DISPATCHED`, `FAILED`) |
| `retry_count` | INTEGER | NOT NULL, DEFAULT 0 | Outbox dispatch retry count |
| `last_error` | TEXT | NULL | Last dispatch error message |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Event creation timestamp |
| `dispatched_at` | TIMESTAMPTZ | NULL | Event dispatch timestamp |

**Indexes**:
- `idx_ingestion_outbox_status` ON `ingestion_outbox(status)`

#### `chunk_embeddings`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique embedding identifier |
| `chunk_id` | UUID | NOT NULL, FK -> `document_chunks(id)` ON DELETE CASCADE | Target chunk |
| `document_id` | UUID | NOT NULL, FK -> `documents(id)` ON DELETE CASCADE | Parent document container |
| `segment_index` | INTEGER | NOT NULL, DEFAULT 0 | 0-based token segment index for chunk |
| `embedding` | VECTOR(768) | NOT NULL | Dense 768-dimensional L2-normalized vector |
| `model_name` | VARCHAR(100) | NOT NULL | Embedding model identifier (`BAAI/bge-base-en-v1.5`) |
| `model_version` | VARCHAR(50) | NOT NULL | Embedding model release/version tag |
| `content_hash` | VARCHAR(64) | NOT NULL | SHA-256 hash of the segment text |
| `token_count` | INTEGER | NOT NULL | Token count of the embedded segment |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Embedding generation timestamp |

**Constraints & Indexes**:
- `uq_chunk_embedding`: UNIQUE(`chunk_id`, `model_name`, `model_version`, `segment_index`)
- `idx_chunk_embeddings_chunk_id` ON `chunk_embeddings(chunk_id)`
- `idx_chunk_embeddings_document_id` ON `chunk_embeddings(document_id)`
- `idx_chunk_embeddings_model` ON `chunk_embeddings(model_name, model_version)`
- `idx_chunk_embeddings_content_hash` ON `chunk_embeddings(content_hash)`
- `idx_chunk_embeddings_hnsw` ON `chunk_embeddings USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 128)`

#### `embedding_jobs`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique job identifier |
| `document_id` | UUID | NOT NULL, FK -> `documents(id)` ON DELETE CASCADE | Target document |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT `'PENDING'` | Status: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` |
| `model_name` | VARCHAR(100) | NOT NULL | Model configured for job |
| `model_version` | VARCHAR(50) | NOT NULL | Model version configured for job |
| `total_chunks` | INTEGER | NOT NULL, DEFAULT 0 | Total document chunks to embed |
| `processed_chunks` | INTEGER | NOT NULL, DEFAULT 0 | Chunks processed so far |
| `total_segments` | INTEGER | NOT NULL, DEFAULT 0 | Total embedded segments persisted |
| `attempts` | INTEGER | NOT NULL, DEFAULT 0 | Worker execution attempt count |
| `max_attempts` | INTEGER | NOT NULL, DEFAULT 3 | Retry limit |
| `error_message` | TEXT | NULL | Captured worker error message |
| `task_id` | VARCHAR(255) | NULL | Celery task ID |
| `started_at` | TIMESTAMPTZ | NULL | Job processing initiation timestamp |
| `completed_at` | TIMESTAMPTZ | NULL | Job completion timestamp |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Job creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Last status update timestamp |

**Indexes**:
- `idx_embedding_jobs_document_id` ON `embedding_jobs(document_id)`
- `idx_embedding_jobs_status` ON `embedding_jobs(status)`

#### `conversations` (Phase 6)
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique conversation identifier |
| `user_id` | UUID | NOT NULL, FK -> `users(id)` ON DELETE CASCADE | Owning student |
| `workspace_id` | UUID | NOT NULL, FK -> `workspaces(id)` ON DELETE CASCADE | Scoped workspace |
| `title` | VARCHAR(255) | NOT NULL, DEFAULT `'New Conversation'` | Discussion topic title |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT `'ACTIVE'` | Conversation status |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Last updated timestamp |
| `last_message_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Timestamp of most recent message |

**Indexes**:
- `idx_conversations_user_workspace` ON `conversations(user_id, workspace_id)`
- `idx_conversations_workspace` ON `conversations(workspace_id)`
- `idx_conversations_last_message` ON `conversations(last_message_at DESC)`

#### `messages` (Phase 6)
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique message identifier |
| `conversation_id` | UUID | NOT NULL, FK -> `conversations(id)` ON DELETE CASCADE | Parent conversation |
| `role` | VARCHAR(30) | NOT NULL | Role: `USER`, `ASSISTANT`, `SYSTEM` |
| `content` | TEXT | NOT NULL | Formatted message content |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT `'COMPLETED'` | Grounding status: `COMPLETED`, `GROUNDED`, `PARTIALLY_GROUNDED`, `INSUFFICIENT_EVIDENCE` |
| `provider` | VARCHAR(50) | NULL | Model provider (`GROQ`, `GEMINI`) |
| `model_name` | VARCHAR(100) | NULL | Specific model name used |
| `input_tokens` | INTEGER | NULL | Total prompt/input token count |
| `output_tokens` | INTEGER | NULL | Total generated token count |
| `latency_ms` | BIGINT | NULL | End-to-end generation latency in ms |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Message timestamp |

**Indexes**:
- `idx_messages_conversation_created` ON `messages(conversation_id, created_at ASC)`

#### `message_sources` (Phase 6)
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique citation link identifier |
| `message_id` | UUID | NOT NULL, FK -> `messages(id)` ON DELETE CASCADE | Parent assistant message |
| `chunk_id` | UUID | NULL, FK -> `document_chunks(id)` ON DELETE SET NULL | Source document chunk |
| `document_id` | UUID | NULL, FK -> `documents(id)` ON DELETE SET NULL | Source document |
| `source_rank` | INTEGER | NOT NULL, DEFAULT 1 | Rank order in context |
| `citation_label` | VARCHAR(50) | NOT NULL | Label referenced in response (`S1`, `S2`) |
| `relevance_score` | DOUBLE PRECISION | NULL | Reranker relevance score |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |

**Indexes**:
- `idx_message_sources_message` ON `message_sources(message_id)`
- `idx_message_sources_chunk` ON `message_sources(chunk_id)`
- `idx_message_sources_doc` ON `message_sources(document_id)`

---

### 2.7. Shared Academic Library (Phase 8)

#### `library_resources` (Phase 8)
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique resource identifier |
| `title` | VARCHAR(255) | NOT NULL | Resource title |
| `description` | TEXT | NOT NULL | Detailed resource description |
| `category` | VARCHAR(100) | NOT NULL | Academic category (e.g. `DATA_STRUCTURES`, `OPERATING_SYSTEMS`) |
| `resource_type` | VARCHAR(100) | NOT NULL | Resource type (e.g. `LECTURE_NOTES`, `TEXTBOOK`, `LAB_MANUAL`) |
| `academic_level` | VARCHAR(100) | NOT NULL, DEFAULT `'ALL_LEVELS'` | Target academic level |
| `language` | VARCHAR(20) | NOT NULL, DEFAULT `'en'` | Resource language |
| `contributor_id` | UUID | NOT NULL, FK -> `users(id)` ON DELETE CASCADE | Contributor identifier |
| `storage_key` | VARCHAR(512) | NOT NULL, UNIQUE | Dedicated isolated S3 key (`library/{id}/...`) |
| `file_name` | VARCHAR(255) | NOT NULL | Document file name |
| `file_type` | VARCHAR(50) | NOT NULL | Document file extension (`PDF`, `DOCX`, etc.) |
| `mime_type` | VARCHAR(100) | NOT NULL | MIME content type |
| `file_size` | BIGINT | NOT NULL | File size in bytes |
| `checksum_sha256` | VARCHAR(64) | NULL | SHA-256 checksum |
| `page_count` | INTEGER | NULL | Page count |
| `rights_type` | VARCHAR(100) | NOT NULL | Rights classification (`ORIGINAL_WORK`, `CREATIVE_COMMONS`, etc.) |
| `license_name` | VARCHAR(255) | NOT NULL | License specification (`CC BY 4.0`, `MIT`, etc.) |
| `source_attribution` | TEXT | NULL | Source attribution and acknowledgements |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT `'DRAFT'` | Status (`DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `REJECTED`, `SUSPENDED`, `REMOVED`) |
| `rejection_reason` | TEXT | NULL | Editorial rejection or suspension feedback |
| `downloads_count` | BIGINT | NOT NULL, DEFAULT 0 | Download counter |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Last updated timestamp |
| `uploaded_at` | TIMESTAMPTZ | NULL | Upload timestamp |
| `published_at` | TIMESTAMPTZ | NULL | Publication approval timestamp |
| `processing_status` | VARCHAR(50) | NOT NULL, DEFAULT `'PENDING_UPLOAD'` | Processing lifecycle status (deferred AI retrieval) |
| `embedding_status` | VARCHAR(50) | NOT NULL, DEFAULT `'NOT_EMBEDDED'` | Embedding lifecycle status (deferred AI retrieval) |
| `search_vector` | tsvector | GENERATED ALWAYS STORED | Generated tsvector on title, description, category, and source_attribution |

**Indexes**:
- `idx_library_resources_status` ON `library_resources(status)`
- `idx_library_resources_category` ON `library_resources(category)`
- `idx_library_resources_type` ON `library_resources(resource_type)`
- `idx_library_resources_contributor` ON `library_resources(contributor_id)`
- `idx_library_resources_published_at` ON `library_resources(published_at)`
- `idx_library_resources_downloads` ON `library_resources(downloads_count DESC)`
- `idx_library_resources_search_vector` ON `library_resources USING gin(search_vector)`

#### `library_resource_tags` (Phase 8)
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique tag identifier |
| `resource_id` | UUID | NOT NULL, FK -> `library_resources(id)` ON DELETE CASCADE | Associated library resource |
| `tag` | VARCHAR(100) | NOT NULL | Tag / topic keyword |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |

**Indexes**:
- `idx_library_resource_tags_resource_id` ON `library_resource_tags(resource_id)`
- `idx_library_resource_tags_tag` ON `library_resource_tags(tag)`
- `uq_library_resource_tag` UNIQUE `(resource_id, tag)`

#### `library_moderation_logs` (Phase 8)
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique audit log identifier |
| `resource_id` | UUID | NOT NULL, FK -> `library_resources(id)` ON DELETE CASCADE | Associated library resource |
| `moderator_id` | UUID | NOT NULL, FK -> `users(id)` ON DELETE CASCADE | Moderator user identifier |
| `action` | VARCHAR(50) | NOT NULL | Action (`SUBMITTED`, `APPROVED`, `REJECTED`, `SUSPENDED`, `RESTORED`) |
| `previous_status` | VARCHAR(50) | NOT NULL | Status before action |
| `new_status` | VARCHAR(50) | NOT NULL | Status after action |
| `reason` | TEXT | NULL | Moderator feedback or justification |
| `notes` | TEXT | NULL | Internal review notes |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Action timestamp |

**Indexes & Constraints**:
- `idx_library_moderation_logs_resource` ON `library_moderation_logs(resource_id)`
- `idx_library_moderation_logs_moderator` ON `library_moderation_logs(moderator_id)`
- `idx_library_moderation_logs_created` ON `library_moderation_logs(created_at DESC)`
- `trg_prevent_moderation_log_modification` TRIGGER: `BEFORE UPDATE OR DELETE` raising PL/pgSQL exception guaranteeing strict audit trail immutability at the database level.

#### `library_resource_reports` (Phase 8)
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique report identifier |
| `resource_id` | UUID | NOT NULL, FK -> `library_resources(id)` ON DELETE CASCADE | Reported resource |
| `reporter_id` | UUID | NOT NULL, FK -> `users(id)` ON DELETE CASCADE | Reporting user |
| `reason_category` | VARCHAR(100) | NOT NULL | Category (`COPYRIGHT_INFRINGEMENT`, `INAPPROPRIATE_CONTENT`, etc.) |
| `explanation` | TEXT | NOT NULL | Reporter explanation |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT `'PENDING'` | Report status (`PENDING`, `RESOLVED`, `DISMISSED`) |
| `resolution_notes` | TEXT | NULL | Moderator resolution notes |
| `resolved_by` | UUID | NULL, FK -> `users(id)` | Moderator who resolved report |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |
| `resolved_at` | TIMESTAMPTZ | NULL | Resolution timestamp |

**Indexes**:
- `idx_library_resource_reports_resource` ON `library_resource_reports(resource_id)`
- `idx_library_resource_reports_reporter` ON `library_resource_reports(reporter_id)`
- `idx_library_resource_reports_status` ON `library_resource_reports(status)`

#### `academic_subjects` (Phase 9)
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique subject identifier |
| `name` | VARCHAR(150) | NOT NULL | Discipline name |
| `slug` | VARCHAR(150) | NOT NULL, UNIQUE | Canonical URL slug |
| `code` | VARCHAR(50) | NOT NULL, UNIQUE | Subject code (e.g. `CS-DSA`) |
| `description` | TEXT | NOT NULL | Overview of subject domain |
| `category` | VARCHAR(100) | NOT NULL | Category grouping |
| `display_order` | INT | NOT NULL, DEFAULT 0 | UI ordering sequence |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Last updated timestamp |

**Indexes**:
- `idx_academic_subjects_slug` ON `academic_subjects(slug)`
- `idx_academic_subjects_code` ON `academic_subjects(code)`
- `idx_academic_subjects_order` ON `academic_subjects(display_order ASC)`

#### `knowledge_topics` (Phase 9)
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique topic identifier |
| `subject_id` | UUID | NOT NULL, FK -> `academic_subjects(id)` ON DELETE RESTRICT | Core academic discipline |
| `parent_topic_id` | UUID | NULL, FK -> `knowledge_topics(id)` ON DELETE SET NULL | Hierarchical parent topic |
| `name` | VARCHAR(200) | NOT NULL | Topic concept title |
| `slug` | VARCHAR(200) | NOT NULL, UNIQUE | URL slug |
| `summary` | TEXT | NOT NULL | Executive concept summary |
| `detailed_description`| TEXT | NULL | In-depth architectural/theoretical explanation |
| `difficulty_level` | VARCHAR(50) | NOT NULL | `BEGINNER`, `INTERMEDIATE`, `ADVANCED` |
| `topic_type` | VARCHAR(50) | NOT NULL | `CORE_CONCEPT`, `DATA_STRUCTURE`, `ALGORITHM`, etc. |
| `estimated_minutes` | INT | NOT NULL, DEFAULT 60 | Estimated study time |
| `importance_score` | FLOAT | NOT NULL, DEFAULT 0.5 | Curriculum importance (0.0–1.0) |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT `'PUBLISHED'` | Lifecycle status |
| `tsv` | TSVECTOR | GENERATED ALWAYS | Generated full-text search vector |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Last updated timestamp |

**Indexes**:
- `idx_knowledge_topics_subject` ON `knowledge_topics(subject_id)`
- `idx_knowledge_topics_parent` ON `knowledge_topics(parent_topic_id)`
- `idx_knowledge_topics_slug` ON `knowledge_topics(slug)`
- `idx_knowledge_topics_difficulty` ON `knowledge_topics(difficulty_level)`
- `idx_knowledge_topics_tsv` GIN (`tsv`)

#### `knowledge_topic_learning_objectives` (Phase 9)
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Identifier |
| `topic_id` | UUID | NOT NULL, FK -> `knowledge_topics(id)` ON DELETE CASCADE | Associated topic |
| `objective` | TEXT | NOT NULL | Concrete learning outcome |
| `display_order` | INT | NOT NULL, DEFAULT 0 | Objective sequence |

#### `knowledge_topic_tags` (Phase 9)
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Identifier |
| `topic_id` | UUID | NOT NULL, FK -> `knowledge_topics(id)` ON DELETE CASCADE | Associated topic |
| `tag` | VARCHAR(100) | NOT NULL | Keyword / tag |

#### `knowledge_topic_relationships` (Phase 9)
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Edge identifier |
| `source_topic_id` | UUID | NOT NULL, FK -> `knowledge_topics(id)` ON DELETE CASCADE | Source topic (e.g. prerequisite) |
| `target_topic_id` | UUID | NOT NULL, FK -> `knowledge_topics(id)` ON DELETE CASCADE | Target topic (e.g. unlocked concept) |
| `relationship_type` | VARCHAR(50) | NOT NULL | `PREREQUISITE_OF`, `RELATED_TO`, `PART_OF`, etc. |
| `weight` | FLOAT | NOT NULL, DEFAULT 1.0 | Relationship strength |
| `description` | TEXT | NULL | Relationship explanation |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |

**Indexes & Constraints**:
- `uk_topic_relationship` UNIQUE (`source_topic_id`, `target_topic_id`, `relationship_type`)
- `chk_no_self_relationship` CHECK (`source_topic_id != target_topic_id`)
- `idx_topic_rel_source` ON `knowledge_topic_relationships(source_topic_id)`
- `idx_topic_rel_target` ON `knowledge_topic_relationships(target_topic_id)`
- `idx_topic_rel_type` ON `knowledge_topic_relationships(relationship_type)`
- `trg_check_prerequisite_cycle` TRIGGER: `BEFORE INSERT OR UPDATE` executing PL/pgSQL function `check_prerequisite_cycle()`. Uses a recursive CTE to detect if `target` can already reach `source` via `PREREQUISITE_OF` edges and acquires a transaction advisory lock `pg_advisory_xact_lock(hashtext('knowledge_prerequisite_cycle_check'))` to prevent race condition-induced cycles during concurrent edge additions.

#### `knowledge_topic_resources` (Phase 9)
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Link identifier |
| `topic_id` | UUID | NOT NULL, FK -> `knowledge_topics(id)` ON DELETE CASCADE | Knowledge topic |
| `resource_id` | UUID | NOT NULL, FK -> `library_resources(id)` ON DELETE CASCADE | Shared library resource (strictly isolated from private documents) |
| `relevance_score` | FLOAT | NOT NULL, DEFAULT 1.0 | Relevance score |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |

---

## 3. Interactive Learning Studio Schema (Phase 10)

The following tables support AI quiz generation, practice attempt evaluation, flashcards, active recall, and SuperMemo-2 (SM-2) spaced repetition scheduling:

#### `quizzes`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Unique quiz identifier |
| `user_id` | UUID | NOT NULL, FK -> `users(id)` ON DELETE CASCADE | Owner of the quiz |
| `workspace_id` | UUID | NULL, FK -> `workspaces(id)` ON DELETE SET NULL | Optional workspace association |
| `title` | VARCHAR(255) | NOT NULL | Quiz title |
| `description` | TEXT | NULL | Academic description / learning objectives |
| `source_type` | VARCHAR(50) | NOT NULL | `WORKSPACE_DOCUMENTS`, `KNOWLEDGE_TOPIC`, `LIBRARY_RESOURCE`, `CUSTOM`, `MANUAL` |
| `source_id` | VARCHAR(255) | NULL | Document ID, topic slug, or resource ID |
| `topic_id` | UUID | NULL, FK -> `knowledge_topics(id)` ON DELETE SET NULL | Knowledge topic reference |
| `difficulty_level` | VARCHAR(50) | NOT NULL, DEFAULT `'MEDIUM'` | `EASY`, `MEDIUM`, `HARD` |
| `total_questions` | INTEGER | NOT NULL, DEFAULT 0 | Count of questions |
| `time_limit_minutes` | INTEGER | NULL | Optional countdown time limit |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Last updated timestamp |

#### `quiz_questions`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Question identifier |
| `quiz_id` | UUID | NOT NULL, FK -> `quizzes(id)` ON DELETE CASCADE | Parent quiz reference |
| `question_index` | INTEGER | NOT NULL | 0-based question ordering |
| `question_text` | TEXT | NOT NULL | Question text |
| `question_type` | VARCHAR(50) | NOT NULL, DEFAULT `'MULTIPLE_CHOICE'` | `MULTIPLE_CHOICE`, `TRUE_FALSE` |
| `difficulty_level` | VARCHAR(50) | NOT NULL, DEFAULT `'MEDIUM'` | `EASY`, `MEDIUM`, `HARD` |
| `explanation` | TEXT | NULL | Academic explanation (revealed ONLY post-submission) |
| `source_reference` | VARCHAR(255) | NULL | Document or topic title reference |
| `page_number` | INTEGER | NULL | Source document page citation |
| `slide_number` | INTEGER | NULL | Source document slide citation |
| `topic_name` | VARCHAR(255) | NULL | Subject topic classification |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |

#### `quiz_question_options`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Option identifier |
| `question_id` | UUID | NOT NULL, FK -> `quiz_questions(id)` ON DELETE CASCADE | Parent question reference |
| `option_index` | INTEGER | NOT NULL | 0-based option index (A, B, C, D) |
| `option_text` | TEXT | NOT NULL | Option prompt text |
| `is_correct` | BOOLEAN | NOT NULL, DEFAULT FALSE | Answer key (omitted from practice DTOs) |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |

#### `quiz_attempts`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Attempt identifier |
| `quiz_id` | UUID | NOT NULL, FK -> `quizzes(id)` ON DELETE CASCADE | Target quiz reference |
| `user_id` | UUID | NOT NULL, FK -> `users(id)` ON DELETE CASCADE | Student user |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT `'IN_PROGRESS'` | `IN_PROGRESS`, `SUBMITTED`, `ABANDONED` |
| `score` | DOUBLE PRECISION | NULL | Final percentage score (0.0 to 100.0) |
| `total_questions` | INTEGER | NOT NULL, DEFAULT 0 | Total questions graded |
| `correct_answers_count`| INTEGER | NOT NULL, DEFAULT 0 | Count of correct choices |
| `time_spent_seconds` | INTEGER | NULL, DEFAULT 0 | Total practice duration |
| `started_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Start timestamp |
| `submitted_at` | TIMESTAMPTZ | NULL | Submission timestamp (locks attempt from changes) |

#### `quiz_attempt_answers`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Student answer identifier |
| `attempt_id` | UUID | NOT NULL, FK -> `quiz_attempts(id)` ON DELETE CASCADE | Attempt session |
| `question_id` | UUID | NOT NULL, FK -> `quiz_questions(id)` ON DELETE CASCADE | Question answered |
| `selected_option_id` | UUID | NULL, FK -> `quiz_question_options(id)` ON DELETE SET NULL | Student's chosen option |
| `selected_text` | TEXT | NULL | Recorded textual response |
| `is_correct` | BOOLEAN | NULL | Graded correctness (evaluated server-side) |
| `marked_for_review` | BOOLEAN | NOT NULL, DEFAULT FALSE | Student review flag |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Last updated timestamp |

#### `flashcard_decks`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Deck identifier |
| `user_id` | UUID | NOT NULL, FK -> `users(id)` ON DELETE CASCADE | Deck creator |
| `workspace_id` | UUID | NULL, FK -> `workspaces(id)` ON DELETE SET NULL | Optional workspace reference |
| `title` | VARCHAR(255) | NOT NULL | Deck title |
| `description` | TEXT | NULL | Deck overview |
| `subject_name` | VARCHAR(255) | NULL | Subject area |
| `topic_id` | UUID | NULL, FK -> `knowledge_topics(id)` ON DELETE SET NULL | Knowledge topic reference |
| `topic_name` | VARCHAR(255) | NULL | Specific topic |
| `source_type` | VARCHAR(50) | NOT NULL, DEFAULT `'MANUAL'` | Generation source |
| `source_id` | VARCHAR(255) | NULL | Source identifier |
| `total_cards` | INTEGER | NOT NULL, DEFAULT 0 | Total cards in deck |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Last updated timestamp |

#### `flashcards`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Card identifier |
| `deck_id` | UUID | NOT NULL, FK -> `flashcard_decks(id)` ON DELETE CASCADE | Parent deck reference |
| `user_id` | UUID | NOT NULL, FK -> `users(id)` ON DELETE CASCADE | User owner |
| `front` | TEXT | NOT NULL | Active recall prompt (question) |
| `back` | TEXT | NOT NULL | Answer & conceptual explanation |
| `topic_name` | VARCHAR(255) | NULL | Topic classification |
| `source_reference` | VARCHAR(255) | NULL | Citation provenance |
| `page_number` | INTEGER | NULL | Source page number |
| `slide_number` | INTEGER | NULL | Source slide number |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Last updated timestamp |

#### `flashcard_review_state` (SM-2 State)
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Scheduling state identifier |
| `card_id` | UUID | NOT NULL, UNIQUE, FK -> `flashcards(id)` ON DELETE CASCADE | Associated flashcard |
| `user_id` | UUID | NOT NULL, FK -> `users(id)` ON DELETE CASCADE | Learner |
| `state` | VARCHAR(50) | NOT NULL, DEFAULT `'NEW'` | `NEW`, `LEARNING`, `REVIEW`, `GRADUATED` |
| `repetition_count` | INTEGER | NOT NULL, DEFAULT 0 | Consecutive successful recalls |
| `ease_factor` | DOUBLE PRECISION | NOT NULL, DEFAULT 2.5, CHECK >= 1.3 | SuperMemo-2 ease factor (clamped >= 1.3) |
| `interval_days` | INTEGER | NOT NULL, DEFAULT 0 | Scheduled repetition interval in days |
| `last_reviewed_at` | TIMESTAMPTZ | NULL | Timestamp of previous review |
| `next_due_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Next review due timestamp |
| `last_rating` | VARCHAR(20) | NULL | `AGAIN`, `HARD`, `GOOD`, `EASY` |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Last updated timestamp |

#### `flashcard_review_history`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Audit log identifier |
| `card_id` | UUID | NOT NULL, FK -> `flashcards(id)` ON DELETE CASCADE | Flashcard |
| `deck_id` | UUID | NOT NULL, FK -> `flashcard_decks(id)` ON DELETE CASCADE | Deck reference |
| `user_id` | UUID | NOT NULL, FK -> `users(id)` ON DELETE CASCADE | Reviewing user |
| `rating` | VARCHAR(20) | NOT NULL | `AGAIN`, `HARD`, `GOOD`, `EASY` |
| `review_time_seconds`| INTEGER | NOT NULL, DEFAULT 0 | Time spent recalling card |
| `scheduled_days` | INTEGER | NOT NULL | Computed interval days |
| `ease_factor` | DOUBLE PRECISION | NOT NULL | Ease factor after review |
| `reviewed_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Review timestamp |

#### `learning_sessions`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY, DEFAULT `gen_random_uuid()` | Session identifier |
| `user_id` | UUID | NOT NULL, FK -> `users(id)` ON DELETE CASCADE | Student user |
| `session_type` | VARCHAR(50) | NOT NULL | `QUIZ_ATTEMPT`, `FLASHCARD_REVIEW` |
| `reference_id` | UUID | NULL | Quiz ID or Deck ID |
| `duration_seconds` | INTEGER | NOT NULL, DEFAULT 0 | Total active study time in seconds |
| `items_completed` | INTEGER | NOT NULL, DEFAULT 0 | Questions answered or cards reviewed |
| `score_percentage` | DOUBLE PRECISION | NULL | Score percentage for quiz sessions |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT `CURRENT_TIMESTAMP` | Session timestamp |

---

## 4. Migration Strategy

- **Tool**: Flyway
- **Location**: `backend/src/main/resources/db/migration/`
- **Executed Migrations**:
  - `V1__init_empty.sql` (Baseline)
  - `V2__create_users_workspaces_auth.sql` (Phase 1)
  - `V3__create_documents.sql` (Phase 2)
  - `V4__reconcile_roles_and_create_ingestion_tables.sql` (Phase 3)
  - `V5__create_chunk_embeddings.sql` (Phase 4)
  - `V6__add_full_text_search_to_chunks.sql` (Phase 5)
  - `V7__create_conversations_and_messages.sql` (Phase 6)
  - `V8__create_shared_academic_library.sql` (Phase 8)
  - `V9__reconcile_library_roles_and_audit_immutability.sql` (Phase 8.1)
  - `V10__create_knowledge_graph_tables.sql` (Phase 9)
  - `V11__seed_knowledge_graph.sql` (Phase 9 - Initial 8 subjects, 45 topics)
  - `V12__expand_curriculum_and_integrity_constraints.sql` (Phase 9.1 - Cycle prevention trigger, advisory lock, 5 additional subjects [CS-ARCH, CS-COMP, CS-TOC, CS-SEC, CS-WEB], 20 new topics [total 65 topics], and DAG integrity constraints)
  - `V13__create_learning_studio_tables.sql` (Phase 10 - Quizzes, flashcards, SM-2 spaced repetition, review history, and learning session analytics)
- **Validation**: Strict validation on startup (`spring.jpa.hibernate.ddl-auto: validate`).
