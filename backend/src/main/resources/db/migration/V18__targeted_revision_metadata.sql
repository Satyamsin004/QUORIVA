-- V18__targeted_revision_metadata.sql
-- CS Scholar AI: Targeted Subject Revision System
-- Adds metadata columns for subject-bounded, material-scoped revision quizzes.

ALTER TABLE quizzes 
    ADD COLUMN IF NOT EXISTS subject_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS target_materials JSONB;

-- Allow TARGETED_REVISION in source_type constraint
ALTER TABLE quizzes DROP CONSTRAINT IF EXISTS chk_quiz_source_type;
ALTER TABLE quizzes ADD CONSTRAINT chk_quiz_source_type 
    CHECK (source_type IN ('WORKSPACE_DOCUMENTS', 'KNOWLEDGE_TOPIC', 'LIBRARY_RESOURCE', 'CUSTOM', 'TARGETED_REVISION'));

-- Indexes for fast lookup of revision history by subject and workspace
CREATE INDEX IF NOT EXISTS idx_quizzes_subject_name ON quizzes(subject_name);
CREATE INDEX IF NOT EXISTS idx_quizzes_subject_workspace ON quizzes(workspace_id, subject_name);

-- Index on document_chunks for ultra-fast page range retrieval
CREATE INDEX IF NOT EXISTS idx_document_chunks_doc_page ON document_chunks(document_id, page_number);
