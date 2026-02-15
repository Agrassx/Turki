-- V3: Add weekly_reports_enabled flag to user_stats
-- Allows users to opt out of weekly report notifications

ALTER TABLE app.user_stats
    ADD COLUMN IF NOT EXISTS weekly_reports_enabled BOOLEAN NOT NULL DEFAULT TRUE;
