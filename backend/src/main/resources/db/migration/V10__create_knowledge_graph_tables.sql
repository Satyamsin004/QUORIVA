-- ==============================================================================
-- Migration: V10__create_knowledge_graph_tables.sql
-- Description: Creates schema for Computer Science Topic Discovery and Knowledge Graph
-- ==============================================================================

-- 1. Academic Subjects Table
CREATE TABLE IF NOT EXISTS academic_subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL UNIQUE,
    slug VARCHAR(150) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    icon_name VARCHAR(50),
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_academic_subjects_slug ON academic_subjects(slug);
CREATE INDEX IF NOT EXISTS idx_academic_subjects_category ON academic_subjects(category);
CREATE INDEX IF NOT EXISTS idx_academic_subjects_display_order ON academic_subjects(display_order);

-- 2. Knowledge Topics Table
CREATE TABLE IF NOT EXISTS knowledge_topics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL REFERENCES academic_subjects(id) ON DELETE CASCADE,
    parent_topic_id UUID REFERENCES knowledge_topics(id) ON DELETE SET NULL,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL UNIQUE,
    summary TEXT NOT NULL,
    description TEXT NOT NULL,
    difficulty_level VARCHAR(50) NOT NULL DEFAULT 'BEGINNER',
    topic_type VARCHAR(50) NOT NULL DEFAULT 'CONCEPT',
    estimated_minutes INT NOT NULL DEFAULT 30,
    status VARCHAR(50) NOT NULL DEFAULT 'PUBLISHED',
    importance_score NUMERIC(3, 2) NOT NULL DEFAULT 1.0,
    search_vector tsvector GENERATED ALWAYS AS (to_tsvector('english', name || ' ' || summary || ' ' || description)) STORED,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_topic_difficulty CHECK (difficulty_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')),
    CONSTRAINT chk_topic_type CHECK (topic_type IN ('SUBJECT', 'TOPIC', 'SUBTOPIC', 'CONCEPT', 'ALGORITHM', 'DATA_STRUCTURE', 'ARCHITECTURE', 'PARADIGM')),
    CONSTRAINT chk_topic_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE INDEX IF NOT EXISTS idx_knowledge_topics_subject ON knowledge_topics(subject_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_topics_parent ON knowledge_topics(parent_topic_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_topics_slug ON knowledge_topics(slug);
CREATE INDEX IF NOT EXISTS idx_knowledge_topics_difficulty ON knowledge_topics(difficulty_level);
CREATE INDEX IF NOT EXISTS idx_knowledge_topics_type ON knowledge_topics(topic_type);
CREATE INDEX IF NOT EXISTS idx_knowledge_topics_status ON knowledge_topics(status);
CREATE INDEX IF NOT EXISTS idx_knowledge_topics_search_vector ON knowledge_topics USING gin(search_vector);

-- 3. Topic Learning Objectives Table
CREATE TABLE IF NOT EXISTS knowledge_topic_learning_objectives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID NOT NULL REFERENCES knowledge_topics(id) ON DELETE CASCADE,
    objective TEXT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_topic_objectives_topic_id ON knowledge_topic_learning_objectives(topic_id);

-- 4. Topic Tags Table
CREATE TABLE IF NOT EXISTS knowledge_topic_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID NOT NULL REFERENCES knowledge_topics(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_knowledge_topic_tag UNIQUE (topic_id, tag)
);

CREATE INDEX IF NOT EXISTS idx_knowledge_topic_tags_topic ON knowledge_topic_tags(topic_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_topic_tags_tag ON knowledge_topic_tags(tag);

-- 5. Topic Relationships Table
CREATE TABLE IF NOT EXISTS knowledge_topic_relationships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_topic_id UUID NOT NULL REFERENCES knowledge_topics(id) ON DELETE CASCADE,
    target_topic_id UUID NOT NULL REFERENCES knowledge_topics(id) ON DELETE CASCADE,
    relationship_type VARCHAR(50) NOT NULL,
    description TEXT,
    is_bidirectional BOOLEAN NOT NULL DEFAULT FALSE,
    weight NUMERIC(3, 2) NOT NULL DEFAULT 1.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_no_self_relationship CHECK (source_topic_id != target_topic_id),
    CONSTRAINT chk_relationship_type CHECK (relationship_type IN (
        'PREREQUISITE_OF', 'RELATED_TO', 'PART_OF', 'EXTENDS', 'USES', 'IMPLEMENTED_BY', 'ALTERNATIVE_TO'
    )),
    CONSTRAINT uq_topic_relationship UNIQUE (source_topic_id, target_topic_id, relationship_type)
);

CREATE INDEX IF NOT EXISTS idx_topic_rel_source ON knowledge_topic_relationships(source_topic_id);
CREATE INDEX IF NOT EXISTS idx_topic_rel_target ON knowledge_topic_relationships(target_topic_id);
CREATE INDEX IF NOT EXISTS idx_topic_rel_type ON knowledge_topic_relationships(relationship_type);
CREATE INDEX IF NOT EXISTS idx_topic_rel_source_type ON knowledge_topic_relationships(source_topic_id, relationship_type);
CREATE INDEX IF NOT EXISTS idx_topic_rel_target_type ON knowledge_topic_relationships(target_topic_id, relationship_type);

-- 6. Topic Library Resource Links Table (Safe connection to Published Shared Library Resources)
CREATE TABLE IF NOT EXISTS knowledge_topic_resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID NOT NULL REFERENCES knowledge_topics(id) ON DELETE CASCADE,
    resource_id UUID NOT NULL REFERENCES library_resources(id) ON DELETE CASCADE,
    relevance_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_topic_resource UNIQUE (topic_id, resource_id)
);

CREATE INDEX IF NOT EXISTS idx_topic_resources_topic ON knowledge_topic_resources(topic_id);
CREATE INDEX IF NOT EXISTS idx_topic_resources_resource ON knowledge_topic_resources(resource_id);

