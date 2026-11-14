-- ==============================================================================
-- CS SCHOLAR AI — Phase 11.1: Token Families & Document Malware Scan Status
-- ==============================================================================

-- 1. Refresh Token Family Identifiers (RFC 6819 Token Family Rotation)
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS family_id UUID;

-- Backfill existing tokens if any
UPDATE refresh_tokens SET family_id = id WHERE family_id IS NULL;

ALTER TABLE refresh_tokens ALTER COLUMN family_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_family_id ON refresh_tokens(family_id);

-- 2. Document Malware Scanning Status
ALTER TABLE documents ADD COLUMN IF NOT EXISTS scan_status VARCHAR(30) NOT NULL DEFAULT 'CLEAN';

ALTER TABLE documents DROP CONSTRAINT IF EXISTS chk_documents_scan_status;
ALTER TABLE documents ADD CONSTRAINT chk_documents_scan_status
    CHECK (scan_status IN ('PENDING', 'CLEAN', 'SUSPICIOUS', 'INFECTED', 'SCAN_FAILED'));

CREATE INDEX IF NOT EXISTS idx_documents_scan_status ON documents(scan_status);

