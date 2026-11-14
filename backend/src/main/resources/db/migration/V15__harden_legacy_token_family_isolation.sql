-- ==============================================================================
-- CS SCHOLAR AI — Phase 11.1.2: Legacy Token Family Isolation & Audit
-- ==============================================================================

-- 1. Explicitly track legacy tokens created prior to token family support
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS is_legacy BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Mark historical tokens where family_id was assigned equal to token id as legacy
UPDATE refresh_tokens SET is_legacy = TRUE WHERE family_id = id;

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_is_legacy ON refresh_tokens(is_legacy);

