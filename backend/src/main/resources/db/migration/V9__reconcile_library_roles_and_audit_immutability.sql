-- ==============================================================================
-- CS SCHOLAR AI — Phase 8.1: Role Reconciliation, Audit Immutability & Processing Status
-- ==============================================================================

-- 1. Reconcile any existing users with role 'INSTRUCTOR' to canonical 'EDUCATOR'
UPDATE users SET role = 'EDUCATOR' WHERE role = 'INSTRUCTOR';

-- 2. Enforce strict check constraint on canonical global roles
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role
    CHECK (role IN ('STUDENT', 'EDUCATOR', 'RESEARCHER', 'ADMIN'));

-- 3. Add AI processing lifecycle status columns to library_resources (for future publication processing)
ALTER TABLE library_resources
    ADD COLUMN IF NOT EXISTS processing_status VARCHAR(50) NOT NULL DEFAULT 'NOT_PROCESSED',
    ADD COLUMN IF NOT EXISTS embedding_status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED';

CREATE INDEX IF NOT EXISTS idx_library_resources_processing ON library_resources(processing_status);

-- 4. Database-Level Immutability Enforcement for Moderation Logs
-- Prevents any UPDATE or DELETE operations on library_moderation_logs
CREATE OR REPLACE FUNCTION prevent_moderation_log_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'library_moderation_logs is strictly immutable and cannot be updated or deleted';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_moderation_log_modification ON library_moderation_logs;
CREATE TRIGGER trg_prevent_moderation_log_modification
BEFORE UPDATE OR DELETE ON library_moderation_logs
FOR EACH ROW
EXECUTE FUNCTION prevent_moderation_log_modification();

