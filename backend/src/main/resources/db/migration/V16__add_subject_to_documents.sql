-- V16__add_subject_to_documents.sql
-- Add subject category field to documents
ALTER TABLE documents ADD COLUMN IF NOT EXISTS subject VARCHAR(100) DEFAULT 'General';
CREATE INDEX IF NOT EXISTS idx_documents_subject ON documents(subject);

