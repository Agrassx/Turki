-- V1: Baseline schema for Turki bot
-- This migration captures the full schema as it existed before Flyway was introduced.
-- For existing databases, this will be applied as a baseline (Flyway baseline).

CREATE SCHEMA IF NOT EXISTS content;
CREATE SCHEMA IF NOT EXISTS app;
CREATE SCHEMA IF NOT EXISTS logs;
CREATE SCHEMA IF NOT EXISTS metrics;
CREATE SCHEMA IF NOT EXISTS billing;

-- ==================== content schema ====================

CREATE TABLE IF NOT EXISTS content.lessons (
    id SERIAL PRIMARY KEY,
    order_index INT NOT NULL,
    target_language VARCHAR(10) NOT NULL,
    level VARCHAR(10) NOT NULL DEFAULT 'A1',
    content_version VARCHAR(32) NOT NULL DEFAULT 'v1',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    content TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS content.vocabulary (
    id SERIAL PRIMARY KEY,
    lesson_id INT NOT NULL REFERENCES content.lessons(id),
    word VARCHAR(255) NOT NULL,
    translation VARCHAR(255) NOT NULL,
    pronunciation VARCHAR(255),
    example TEXT
);

CREATE TABLE IF NOT EXISTS content.homeworks (
    id SERIAL PRIMARY KEY,
    lesson_id INT NOT NULL REFERENCES content.lessons(id)
);

CREATE TABLE IF NOT EXISTS content.homework_questions (
    id SERIAL PRIMARY KEY,
    homework_id INT NOT NULL REFERENCES content.homeworks(id),
    question_type VARCHAR(50) NOT NULL,
    question_text TEXT NOT NULL,
    options TEXT NOT NULL DEFAULT '[]',
    correct_answer TEXT NOT NULL
);

-- ==================== app schema ====================

CREATE TABLE IF NOT EXISTS app.users (
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT NOT NULL,
    username VARCHAR(255),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255),
    language VARCHAR(10) NOT NULL DEFAULT 'ru',
    subscription_active BOOLEAN NOT NULL DEFAULT FALSE,
    subscription_expires_at TIMESTAMPTZ,
    current_lesson_id INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS users_telegram_id_unique ON app.users(telegram_id);

CREATE TABLE IF NOT EXISTS app.homework_submissions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app.users(id),
    homework_id INT NOT NULL REFERENCES content.homeworks(id),
    answers TEXT NOT NULL,
    score INT NOT NULL,
    max_score INT NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS app.reminders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app.users(id),
    type VARCHAR(50) NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    sent BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS app.user_states (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app.users(id),
    state VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL DEFAULT '{}',
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS user_states_user_id_unique ON app.user_states(user_id);

CREATE TABLE IF NOT EXISTS app.user_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app.users(id),
    lesson_id INT NOT NULL REFERENCES content.lessons(id),
    status VARCHAR(32) NOT NULL,
    last_exercise_id INT,
    content_version VARCHAR(32) NOT NULL DEFAULT 'v1',
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS user_progress_user_id ON app.user_progress(user_id);
CREATE INDEX IF NOT EXISTS user_progress_lesson_id ON app.user_progress(lesson_id);
CREATE UNIQUE INDEX IF NOT EXISTS user_progress_user_lesson ON app.user_progress(user_id, lesson_id);

CREATE TABLE IF NOT EXISTS app.user_dictionary (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app.users(id),
    vocabulary_id INT NOT NULL REFERENCES content.vocabulary(id),
    is_favorite BOOLEAN NOT NULL DEFAULT TRUE,
    tags TEXT NOT NULL DEFAULT '[]',
    added_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS user_dictionary_user_id ON app.user_dictionary(user_id);
CREATE INDEX IF NOT EXISTS user_dictionary_vocabulary_id ON app.user_dictionary(vocabulary_id);
CREATE UNIQUE INDEX IF NOT EXISTS user_dictionary_user_vocab ON app.user_dictionary(user_id, vocabulary_id);

CREATE TABLE IF NOT EXISTS app.user_custom_dictionary (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app.users(id),
    word VARCHAR(255) NOT NULL,
    translation VARCHAR(255) NOT NULL,
    pronunciation VARCHAR(255),
    example TEXT,
    added_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS user_custom_dictionary_user_id ON app.user_custom_dictionary(user_id);

CREATE TABLE IF NOT EXISTS app.review_cards (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app.users(id),
    vocabulary_id INT NOT NULL REFERENCES content.vocabulary(id),
    stage INT NOT NULL DEFAULT 0,
    next_review_at TIMESTAMPTZ NOT NULL,
    last_result BOOLEAN
);

CREATE INDEX IF NOT EXISTS review_cards_user_id ON app.review_cards(user_id);
CREATE INDEX IF NOT EXISTS review_cards_vocabulary_id ON app.review_cards(vocabulary_id);
CREATE UNIQUE INDEX IF NOT EXISTS review_cards_user_vocab ON app.review_cards(user_id, vocabulary_id);

CREATE TABLE IF NOT EXISTS app.reminder_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app.users(id),
    days_of_week VARCHAR(32) NOT NULL,
    time_local VARCHAR(8) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_fired_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS reminder_preferences_user_id ON app.reminder_preferences(user_id);

CREATE TABLE IF NOT EXISTS app.user_stats (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app.users(id),
    current_streak INT NOT NULL DEFAULT 0,
    last_active_at TIMESTAMPTZ,
    weekly_lessons INT NOT NULL DEFAULT 0,
    weekly_practice INT NOT NULL DEFAULT 0,
    weekly_review INT NOT NULL DEFAULT 0,
    weekly_homework INT NOT NULL DEFAULT 0,
    last_weekly_report_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS user_stats_user_id ON app.user_stats(user_id);

-- ==================== logs schema ====================

CREATE TABLE IF NOT EXISTS logs.analytics_events (
    id BIGSERIAL PRIMARY KEY,
    event_name VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES app.users(id),
    session_id VARCHAR(64),
    props TEXT NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS analytics_events_user_id ON logs.analytics_events(user_id);

CREATE TABLE IF NOT EXISTS logs.metrics_snapshots (
    id BIGSERIAL PRIMARY KEY,
    date VARCHAR(10) NOT NULL,
    metric_name VARCHAR(64) NOT NULL,
    value BIGINT NOT NULL,
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS metrics_snapshots_date ON logs.metrics_snapshots(date);
CREATE INDEX IF NOT EXISTS metrics_snapshots_metric_name ON logs.metrics_snapshots(metric_name);

CREATE TABLE IF NOT EXISTS logs.error_logs (
    id BIGSERIAL PRIMARY KEY,
    error_type VARCHAR(128) NOT NULL,
    message TEXT NOT NULL,
    stack_trace TEXT,
    user_id BIGINT,
    context TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS error_logs_user_id ON logs.error_logs(user_id);
CREATE INDEX IF NOT EXISTS error_logs_created_at ON logs.error_logs(created_at);

-- ==================== billing schema ====================

CREATE TABLE IF NOT EXISTS billing.subscription_plans (
    id SERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    price_monthly BIGINT NOT NULL DEFAULT 0,
    price_yearly BIGINT NOT NULL DEFAULT 0,
    max_lessons INT,
    max_reviews_per_day INT,
    max_practice_per_day INT,
    has_ads BOOLEAN NOT NULL DEFAULT TRUE,
    has_offline_access BOOLEAN NOT NULL DEFAULT FALSE,
    has_priority_support BOOLEAN NOT NULL DEFAULT FALSE,
    features TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS subscription_plans_code ON billing.subscription_plans(code);

CREATE TABLE IF NOT EXISTS billing.user_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app.users(id),
    plan_id INT NOT NULL REFERENCES billing.subscription_plans(id),
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    payment_provider VARCHAR(32),
    payment_provider_id VARCHAR(255),
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS user_subscriptions_user_id ON billing.user_subscriptions(user_id);

CREATE TABLE IF NOT EXISTS billing.payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app.users(id),
    subscription_id BIGINT REFERENCES billing.user_subscriptions(id),
    amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'RUB',
    status VARCHAR(32) NOT NULL,
    payment_provider VARCHAR(32) NOT NULL,
    payment_provider_id VARCHAR(255),
    description TEXT,
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS payment_transactions_user_id ON billing.payment_transactions(user_id);
