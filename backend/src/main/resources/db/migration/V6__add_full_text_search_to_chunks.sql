-- ==============================================================================
-- CS SCHOLAR AI — Phase 5: PostgreSQL Full-Text Search Schema
-- ==============================================================================

-- 1. Add generated tsvector column for document_chunks.content using English dictionary
ALTER TABLE document_chunks
    ADD COLUMN IF NOT EXISTS content_tsv tsvector
    GENERATED ALWAYS AS (to_tsvector('english', content)) STORED;

-- 2. Create GIN index for fast full-text keyword searches
CREATE INDEX IF NOT EXISTS idx_document_chunks_content_tsv
    ON document_chunks
    USING gin(content_tsv);

