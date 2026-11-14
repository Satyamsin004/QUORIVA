-- CS Scholar AI - PostgreSQL Initialization Script
-- Enable pgvector extension for vector embeddings storage and semantic search

CREATE EXTENSION IF NOT EXISTS vector;

-- Verification log output
DO $$
BEGIN
    RAISE NOTICE 'pgvector extension successfully initialized for CS Scholar AI database.';
END $$;

