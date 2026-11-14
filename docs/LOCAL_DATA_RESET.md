# CS Scholar AI — Local Development Data Reset & Clean-State Runbook

**Document Version**: `1.0.0`  
**Target Environment**: Local Development ONLY (`localhost` / `127.0.0.1`)  
**Scope**: PostgreSQL database records, MinIO object storage, Redis caches and Celery task queues.

---

## 1. Overview & Objective

This runbook documents the safe, controlled, and repeatable procedure to purge all user-generated, test, and session data from the local CS Scholar AI development environment.

The objective is to return the local platform to a clean, pristine initial state—exactly as if a brand-new user has accessed CS Scholar AI for the first time—while strictly preserving:
- All database schemas, tables, constraints, triggers, and extensions.
- All 15 Flyway migration history records (`flyway_schema_history`).
- All curated Computer Science Curriculum reference data (13 `academic_subjects`, 65 `knowledge_topics`, 64 `learning_objectives`, 81 `relationships`, 98 `tags`).
- All MinIO bucket structures and access configurations.
- All application source code, automated test suites, and RAG benchmark datasets.

---

## 2. Environment Safety & Guards

To prevent accidental execution against remote, staging, or production environments, the reset procedure enforces strict programmatic guards:

1. **Host Verification**: The script inspects `POSTGRES_HOST`, `REDIS_HOST`, and `S3_ENDPOINT`. Execution aborts immediately if any target is not in `{"localhost", "127.0.0.1", "::1"}`.
2. **Database Identity**: The database name must strictly match `cs_scholar_db`.
3. **No Destructive DDL**: The procedure does NOT execute `DROP DATABASE`, `DROP SCHEMA`, `DROP OWNED`, or `docker compose down -v`.
4. **Selective Redis Targeting**: The script does NOT execute global `FLUSHALL` or `FLUSHDB`, ensuring shared local Redis instances are not disrupted.
5. **Interactive Confirmation Guard**: The script requires an explicit `--confirm` flag or interactive user confirmation before executing any delete operation.

---

## 3. Data Inventory & Retention Classification

### 3.1 Preserved Static Reference Data (Never Deleted)

The following tables contain canonical computer science academic curriculum data and schema migration tracking. They are intentionally retained:

| Table Name | Retained Rows | Purpose / Content |
| :--- | :---: | :--- |
| `academic_subjects` | 13 | Canonical CS subjects (Algorithms, OS, Networks, DBMS, AI/ML, Compilers, Architecture, etc.) |
| `knowledge_topics` | 65 | Core curriculum topics seeded via Flyway V11 and V12 |
| `knowledge_topic_learning_objectives` | 64 | Granular learning goals per topic |
| `knowledge_topic_relationships` | 81 | Topic prerequisite DAG edges with cycle prevention triggers |
| `knowledge_topic_tags` | 98 | Search and taxonomy indexing tags |
| `flyway_schema_history` | 15 | Migrations V1 through V15 tracking schema integrity |

### 3.2 Purged User & Test Data Tables (28 Tables)

All user-generated records are removed in reverse dependency order inside a single database transaction (`BEGIN ... COMMIT`):

```
1.  quiz_attempt_answers        (Leaf: Student quiz answers)
2.  quiz_attempts               (Student quiz sessions)
3.  quiz_question_options       (Generated quiz options)
4.  quiz_questions              (Generated quiz questions)
5.  quizzes                     (Quiz metadata)
6.  flashcard_review_history    (Spaced repetition review logs)
7.  flashcard_review_state      (SM-2 card state: interval, EF, repetitions)
8.  flashcards                  (Flashcard items)
9.  flashcard_decks             (Flashcard deck collections)
10. learning_sessions           (Study session tracking)
11. library_resource_reports    (Moderation flag reports)
12. library_moderation_logs     (Audit moderation history - trigger bypassed temporarily)
13. library_resource_tags       (Library metadata tags)
14. knowledge_topic_resources   (User-linked topic resources)
15. library_resources           (Community shared library submissions)
16. message_sources             (RAG chunk attribution references)
17. messages                    (Tutor dialogue turns)
18. conversations               (Workspace chat threads)
19. chunk_embeddings            (768-dim pgvector vector rows)
20. document_chunks             (Extracted text chunks & token counts)
21. embedding_jobs              (Background embedding queue records)
22. ingestion_outbox            (Asynchronous ingestion outbox queue)
23. processing_jobs             (FastAPI document processing records)
24. documents                   (Document metadata & S3 keys)
25. workspace_members           (Multi-tenant workspace access mappings)
26. workspaces                  (User private workspaces)
27. refresh_tokens              (RFC 6819 token families)
28. users                       (Student, Educator, Admin user accounts)
```

> [!NOTE]
> `library_moderation_logs` is protected by a database trigger `trg_prevent_moderation_log_modification` that prevents runtime tampering. The reset script disables this trigger temporarily within the transaction and re-enables it immediately after deletion.

---

## 4. Reset Script Usage

### 4.1 Dry Run (Pre-Purge Inventory Inspection)

To inspect current database row counts, MinIO object keys, and Redis keys without modifying anything:

**PowerShell (Windows):**
```powershell
powershell -ExecutionPolicy Bypass -File infrastructure/scripts/reset-local-data.ps1 -DryRun
```

**Bash (Linux / macOS):**
```bash
./infrastructure/scripts/reset-local-data.sh --dry-run
```

### 4.2 Executing the Reset

**Interactive Execution:**
```powershell
powershell -ExecutionPolicy Bypass -File infrastructure/scripts/reset-local-data.ps1
```

**Non-Interactive Execution (Automated / CI):**
```powershell
powershell -ExecutionPolicy Bypass -File infrastructure/scripts/reset-local-data.ps1 -Confirm
```

### 4.3 Python Direct Invocation

```powershell
.\ai-service\.venv\Scripts\python infrastructure/scripts/reset_local_data.py --confirm
```

---

## 5. Storage & Cache Reset Procedures

### 5.1 MinIO S3 Object Storage
- The script connects to `http://localhost:9000` via the AWS S3 v4 protocol.
- All objects within the application bucket (`cs-scholar-documents`) and temporary test buckets (`cs-scholar-test-bucket`) are removed.
- The `cs-scholar-documents` bucket itself is preserved with its access policies intact, ready for immediate subsequent uploads.

### 5.2 Redis Queues & Replay Nonces
- The Celery task list (`celery`) is deleted, clearing any pending background ingestion or embedding jobs.
- Celery exchange bindings (`_kombu.binding.celery`, `_kombu.binding_permanent.celery`) are reset.
- Internal service replay nonces (`cs:replay:nonce:*`) and test keys (`phase0_test`) are purged.
- No other databases or keys on the Redis instance are touched.

---

## 6. Post-Reset Verification Checklist

Following a reset, verify the clean-state baseline:

1. **Database Schema & Migrations**:
   ```powershell
   docker exec cs-scholar-postgres psql -U postgres -d cs_scholar_db -c "SELECT count(*) FROM flyway_schema_history WHERE success = true;"
   # Expected: 15
   ```
2. **User Data Row Counts**:
   ```powershell
   docker exec cs-scholar-postgres psql -U postgres -d cs_scholar_db -c "SELECT count(*) FROM users; SELECT count(*) FROM documents;"
   # Expected: 0, 0
   ```
3. **Static Reference Integrity**:
   ```powershell
   docker exec cs-scholar-postgres psql -U postgres -d cs_scholar_db -c "SELECT count(*) FROM academic_subjects; SELECT count(*) FROM knowledge_topics;"
   # Expected: 13, 65
   ```
4. **MinIO Object Count**:
   ```powershell
   .\ai-service\.venv\Scripts\python -c "import boto3; s3 = boto3.client('s3', endpoint_url='http://localhost:9000', aws_access_key_id='minioadmin', aws_secret_access_key='minioadmin'); print('Objects:', len(s3.list_objects_v2(Bucket='cs-scholar-documents').get('Contents', [])))"
   # Expected: Objects: 0
   ```
5. **Backend Verification Integration Test**:
   ```powershell
   cd backend; mvn test -Dtest=CleanStateVerificationIntegrationTest
   # Expected: BUILD SUCCESS (Tests run: 1, Failures: 0)
   ```

