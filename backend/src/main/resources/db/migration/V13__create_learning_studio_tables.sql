-- V13__create_learning_studio_tables.sql
-- CS Scholar AI: Phase 10 Interactive Learning Studio
-- Quizzes, Questions, Options, Attempts, Flashcard Decks, Flashcards,
-- Spaced Repetition (SM-2), and Learning Analytics.

-- 1. Quizzes
CREATE TABLE quizzes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    workspace_id UUID REFERENCES workspaces(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    source_type VARCHAR(50) NOT NULL, -- WORKSPACE_DOCUMENTS, KNOWLEDGE_TOPIC, LIBRARY_RESOURCE, CUSTOM
    source_id VARCHAR(255),
    topic_id UUID REFERENCES knowledge_topics(id) ON DELETE SET NULL,
    difficulty_level VARCHAR(50) NOT NULL DEFAULT 'MEDIUM', -- EASY, MEDIUM, HARD
    total_questions INT NOT NULL DEFAULT 0,
    time_limit_minutes INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_quiz_difficulty CHECK (difficulty_level IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT chk_quiz_source_type CHECK (source_type IN ('WORKSPACE_DOCUMENTS', 'KNOWLEDGE_TOPIC', 'LIBRARY_RESOURCE', 'CUSTOM'))
);

CREATE INDEX idx_quizzes_user_id ON quizzes(user_id);
CREATE INDEX idx_quizzes_workspace_id ON quizzes(workspace_id);
CREATE INDEX idx_quizzes_topic_id ON quizzes(topic_id);
CREATE INDEX idx_quizzes_created_at ON quizzes(created_at DESC);

-- 2. Quiz Questions
CREATE TABLE quiz_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    question_index INT NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(50) NOT NULL DEFAULT 'MULTIPLE_CHOICE', -- MULTIPLE_CHOICE, TRUE_FALSE
    difficulty_level VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    explanation TEXT NOT NULL,
    source_reference VARCHAR(512),
    page_number INT,
    slide_number INT,
    topic_name VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_quiz_question_index UNIQUE (quiz_id, question_index),
    CONSTRAINT chk_question_type CHECK (question_type IN ('MULTIPLE_CHOICE', 'TRUE_FALSE')),
    CONSTRAINT chk_question_difficulty CHECK (difficulty_level IN ('EASY', 'MEDIUM', 'HARD'))
);

CREATE INDEX idx_quiz_questions_quiz_id ON quiz_questions(quiz_id);

-- 3. Quiz Question Options
CREATE TABLE quiz_question_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    option_index INT NOT NULL,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_quiz_option_index UNIQUE (question_id, option_index)
);

CREATE INDEX idx_quiz_options_question_id ON quiz_question_options(question_id);

-- 4. Quiz Attempts
CREATE TABLE quiz_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, SUBMITTED, ABANDONED
    score FLOAT,
    total_questions INT NOT NULL DEFAULT 0,
    correct_answers_count INT NOT NULL DEFAULT 0,
    time_spent_seconds INT DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMPTZ,
    CONSTRAINT chk_attempt_status CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'ABANDONED'))
);

CREATE INDEX idx_quiz_attempts_user ON quiz_attempts(user_id);
CREATE INDEX idx_quiz_attempts_quiz ON quiz_attempts(quiz_id);
CREATE INDEX idx_quiz_attempts_status ON quiz_attempts(status);
CREATE INDEX idx_quiz_attempts_started ON quiz_attempts(started_at DESC);

-- 5. Quiz Attempt Answers
CREATE TABLE quiz_attempt_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    selected_option_id UUID REFERENCES quiz_question_options(id) ON DELETE SET NULL,
    selected_text TEXT,
    is_correct BOOLEAN,
    is_marked_for_review BOOLEAN NOT NULL DEFAULT FALSE,
    answered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_attempt_question UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_attempt_answers_attempt ON quiz_attempt_answers(attempt_id);

-- 6. Flashcard Decks
CREATE TABLE flashcard_decks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    workspace_id UUID REFERENCES workspaces(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    subject_name VARCHAR(150),
    topic_id UUID REFERENCES knowledge_topics(id) ON DELETE SET NULL,
    topic_name VARCHAR(255),
    source_type VARCHAR(50) NOT NULL DEFAULT 'MANUAL', -- WORKSPACE_DOCUMENTS, KNOWLEDGE_TOPIC, LIBRARY_RESOURCE, MANUAL
    source_id VARCHAR(255),
    total_cards INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_deck_source_type CHECK (source_type IN ('WORKSPACE_DOCUMENTS', 'KNOWLEDGE_TOPIC', 'LIBRARY_RESOURCE', 'MANUAL'))
);

CREATE INDEX idx_flashcard_decks_user ON flashcard_decks(user_id);
CREATE INDEX idx_flashcard_decks_workspace ON flashcard_decks(workspace_id);
CREATE INDEX idx_flashcard_decks_topic ON flashcard_decks(topic_id);
CREATE INDEX idx_flashcard_decks_updated ON flashcard_decks(updated_at DESC);

-- 7. Flashcards
CREATE TABLE flashcards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    deck_id UUID NOT NULL REFERENCES flashcard_decks(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    front TEXT NOT NULL,
    back TEXT NOT NULL,
    topic_name VARCHAR(255),
    source_reference VARCHAR(512),
    page_number INT,
    slide_number INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_flashcards_deck_id ON flashcards(deck_id);
CREATE INDEX idx_flashcards_user_id ON flashcards(user_id);

-- 8. Flashcard Review State (SM-2 Algorithm parameters)
CREATE TABLE flashcard_review_state (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id UUID NOT NULL UNIQUE REFERENCES flashcards(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    state VARCHAR(50) NOT NULL DEFAULT 'NEW', -- NEW, LEARNING, REVIEW, GRADUATED
    repetition_count INT NOT NULL DEFAULT 0,
    ease_factor FLOAT NOT NULL DEFAULT 2.5,
    interval_days INT NOT NULL DEFAULT 0,
    last_reviewed_at TIMESTAMPTZ,
    next_due_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_rating VARCHAR(20), -- AGAIN, HARD, GOOD, EASY
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_review_state CHECK (state IN ('NEW', 'LEARNING', 'REVIEW', 'GRADUATED')),
    CONSTRAINT chk_last_rating CHECK (last_rating IS NULL OR last_rating IN ('AGAIN', 'HARD', 'GOOD', 'EASY')),
    CONSTRAINT chk_ease_factor_bounds CHECK (ease_factor >= 1.3)
);

CREATE INDEX idx_review_state_user_due ON flashcard_review_state(user_id, next_due_at ASC);
CREATE INDEX idx_review_state_card_id ON flashcard_review_state(card_id);
CREATE INDEX idx_review_state_user_state ON flashcard_review_state(user_id, state);

-- 9. Flashcard Review History
CREATE TABLE flashcard_review_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id UUID NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
    deck_id UUID NOT NULL REFERENCES flashcard_decks(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating VARCHAR(20) NOT NULL, -- AGAIN, HARD, GOOD, EASY
    review_time_seconds INT DEFAULT 0,
    scheduled_days INT NOT NULL DEFAULT 0,
    ease_factor FLOAT NOT NULL,
    reviewed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_history_rating CHECK (rating IN ('AGAIN', 'HARD', 'GOOD', 'EASY'))
);

CREATE INDEX idx_review_history_user_date ON flashcard_review_history(user_id, reviewed_at DESC);
CREATE INDEX idx_review_history_deck ON flashcard_review_history(deck_id);
CREATE INDEX idx_review_history_card ON flashcard_review_history(card_id);

-- 10. Learning Sessions (Aggregated study sessions for analytics)
CREATE TABLE learning_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_type VARCHAR(50) NOT NULL, -- QUIZ_ATTEMPT, FLASHCARD_REVIEW
    reference_id UUID NOT NULL, -- quiz_attempt_id or deck_id
    duration_seconds INT NOT NULL DEFAULT 0,
    items_completed INT NOT NULL DEFAULT 0,
    score_percentage FLOAT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_session_type CHECK (session_type IN ('QUIZ_ATTEMPT', 'FLASHCARD_REVIEW'))
);

CREATE INDEX idx_learning_sessions_user_date ON learning_sessions(user_id, created_at DESC);

