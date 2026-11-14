-- ==============================================================================
-- CS SCHOLAR AI — Phase 8: Shared Academic Library Schema
-- ==============================================================================

-- 1. Shared Academic Library Resources Table
-- Note: Dedicated storage_key prefix 'library/{resourceId}/{filename}'
-- completely isolated from private workspace documents.
CREATE TABLE IF NOT EXISTS library_resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    academic_level VARCHAR(100) NOT NULL DEFAULT 'ALL_LEVELS',
    language VARCHAR(20) NOT NULL DEFAULT 'en',
    contributor_id UUID NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64),
    page_count INTEGER,
    rights_type VARCHAR(100) NOT NULL,
    license_name VARCHAR(255) NOT NULL,
    source_attribution TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    rejection_reason TEXT,
    downloads_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    search_vector tsvector GENERATED ALWAYS AS (
        to_tsvector('english',
            coalesce(title, '') || ' ' ||
            coalesce(description, '') || ' ' ||
            coalesce(category, '') || ' ' ||
            coalesce(source_attribution, '')
        )
    ) STORED,
    CONSTRAINT fk_library_resources_contributor FOREIGN KEY (contributor_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_library_resources_storage_key UNIQUE (storage_key)
);

CREATE INDEX IF NOT EXISTS idx_library_resources_status ON library_resources(status);
CREATE INDEX IF NOT EXISTS idx_library_resources_category ON library_resources(category);
CREATE INDEX IF NOT EXISTS idx_library_resources_type ON library_resources(resource_type);
CREATE INDEX IF NOT EXISTS idx_library_resources_contributor ON library_resources(contributor_id);
CREATE INDEX IF NOT EXISTS idx_library_resources_published_at ON library_resources(published_at);
CREATE INDEX IF NOT EXISTS idx_library_resources_downloads ON library_resources(downloads_count DESC);
CREATE INDEX IF NOT EXISTS idx_library_resources_search_vector ON library_resources USING gin(search_vector);

-- 2. Library Resource Tags Table
CREATE TABLE IF NOT EXISTS library_resource_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_id UUID NOT NULL REFERENCES library_resources(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_library_resource_tag UNIQUE (resource_id, tag)
);

CREATE INDEX IF NOT EXISTS idx_library_resource_tags_resource_id ON library_resource_tags(resource_id);
CREATE INDEX IF NOT EXISTS idx_library_resource_tags_tag ON library_resource_tags(tag);

-- 3. Moderation Audit Logs Table
CREATE TABLE IF NOT EXISTS library_moderation_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_id UUID NOT NULL REFERENCES library_resources(id) ON DELETE CASCADE,
    moderator_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    previous_status VARCHAR(50) NOT NULL,
    new_status VARCHAR(50) NOT NULL,
    reason TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_library_moderation_logs_resource ON library_moderation_logs(resource_id);
CREATE INDEX IF NOT EXISTS idx_library_moderation_logs_moderator ON library_moderation_logs(moderator_id);
CREATE INDEX IF NOT EXISTS idx_library_moderation_logs_created ON library_moderation_logs(created_at DESC);

-- 4. Resource Reports Table
CREATE TABLE IF NOT EXISTS library_resource_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_id UUID NOT NULL REFERENCES library_resources(id) ON DELETE CASCADE,
    reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason_category VARCHAR(100) NOT NULL,
    explanation TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    resolution_notes TEXT,
    resolved_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_library_resource_reports_resource ON library_resource_reports(resource_id);
CREATE INDEX IF NOT EXISTS idx_library_resource_reports_reporter ON library_resource_reports(reporter_id);
CREATE INDEX IF NOT EXISTS idx_library_resource_reports_status ON library_resource_reports(status);

