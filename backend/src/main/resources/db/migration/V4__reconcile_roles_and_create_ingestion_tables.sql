-- ==============================================================================
-- CS SCHOLAR AI — Phase 3: Ingestion Schema & Role Reconciliation
-- ==============================================================================

-- 1. Reconcile Workspace Role Names (Standardize to OWNER, EDITOR, VIEWER)
UPDATE workspace_members SET member_role = 'EDITOR' WHERE member_role = 'MEMBER';
ALTER TABLE workspace_members ALTER COLUMN member_role SET DEFAULT 'EDITOR';

-- 2. Document Chunks Table
CREATE TABLE IF NOT EXISTS document_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    processing_version INTEGER NOT NULL DEFAULT 1,
    page_number INTEGER,
    slide_number INTEGER,
    section_title VARCHAR(255),
    content TEXT NOT NULL,
    token_count INTEGER NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    extraction_method VARCHAR(50) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_chunks_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT uq_document_chunk UNIQUE (document_id, processing_version, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_document_chunks_version ON document_chunks(document_id, processing_version);
CREATE INDEX IF NOT EXISTS idx_document_chunks_page ON document_chunks(document_id, page_number);
CREATE INDEX IF NOT EXISTS idx_document_chunks_hash ON document_chunks(content_hash);

-- 3. Processing Jobs Table
CREATE TABLE IF NOT EXISTS processing_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'QUEUED',
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    error_message TEXT,
    task_id VARCHAR(255),
    processing_version INTEGER NOT NULL DEFAULT 1,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_processing_jobs_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_processing_jobs_document_id ON processing_jobs(document_id);
CREATE INDEX IF NOT EXISTS idx_processing_jobs_status ON processing_jobs(status);

-- 4. Transactional Ingestion Outbox Table
CREATE TABLE IF NOT EXISTS ingestion_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL DEFAULT 'INGESTION_REQUESTED',
    payload JSONB NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dispatched_at TIMESTAMPTZ,
    CONSTRAINT fk_ingestion_outbox_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ingestion_outbox_status ON ingestion_outbox(status);

