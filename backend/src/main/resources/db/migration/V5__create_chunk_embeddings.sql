-- ==============================================================================
-- CS SCHOLAR AI — Phase 4: Embedding Generation & pgvector Schema
-- ==============================================================================

-- 1. Chunk Embeddings Table (pgvector 768-dimensional normalized vectors)
CREATE TABLE IF NOT EXISTS chunk_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chunk_id UUID NOT NULL,
    document_id UUID NOT NULL,
    segment_index INTEGER NOT NULL DEFAULT 0,
    embedding VECTOR(768) NOT NULL,
    model_name VARCHAR(150) NOT NULL,
    model_version VARCHAR(100) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    token_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chunk_embeddings_chunk FOREIGN KEY (chunk_id) REFERENCES document_chunks(id) ON DELETE CASCADE,
    CONSTRAINT fk_chunk_embeddings_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT uq_chunk_embedding UNIQUE (chunk_id, model_name, model_version, segment_index)
);

CREATE INDEX IF NOT EXISTS idx_chunk_embeddings_chunk_id ON chunk_embeddings(chunk_id);
CREATE INDEX IF NOT EXISTS idx_chunk_embeddings_document_id ON chunk_embeddings(document_id);
CREATE INDEX IF NOT EXISTS idx_chunk_embeddings_model_version ON chunk_embeddings(model_name, model_version);
CREATE INDEX IF NOT EXISTS idx_chunk_embeddings_content_hash ON chunk_embeddings(content_hash);

-- HNSW Vector Index for Cosine Distance Search
CREATE INDEX IF NOT EXISTS idx_chunk_embeddings_hnsw
ON chunk_embeddings
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 128);

-- 2. Embedding Jobs Table (Asynchronous tracking for embedding generation)
CREATE TABLE IF NOT EXISTS embedding_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL,
    job_type VARCHAR(50) NOT NULL DEFAULT 'EMBEDDING_GENERATION',
    status VARCHAR(50) NOT NULL DEFAULT 'QUEUED',
    model_name VARCHAR(150) NOT NULL,
    model_version VARCHAR(100) NOT NULL,
    processing_version INTEGER NOT NULL DEFAULT 1,
    chunks_total INTEGER NOT NULL DEFAULT 0,
    chunks_processed INTEGER NOT NULL DEFAULT 0,
    embeddings_generated INTEGER NOT NULL DEFAULT 0,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    error_message TEXT,
    task_id VARCHAR(255),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_embedding_jobs_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_embedding_jobs_document_id ON embedding_jobs(document_id);
CREATE INDEX IF NOT EXISTS idx_embedding_jobs_status ON embedding_jobs(status);

-- 3. Add embedding_status to documents table
ALTER TABLE documents ADD COLUMN IF NOT EXISTS embedding_status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED';
CREATE INDEX IF NOT EXISTS idx_documents_embedding_status ON documents(embedding_status);

