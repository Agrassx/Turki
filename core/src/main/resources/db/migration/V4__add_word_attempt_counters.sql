-- V4: Add attempt counters to review_cards for word mastery statistics
-- Tracks how many times each word was attempted and how many were correct

ALTER TABLE app.review_cards
    ADD COLUMN IF NOT EXISTS correct_count INT NOT NULL DEFAULT 0;

ALTER TABLE app.review_cards
    ADD COLUMN IF NOT EXISTS total_attempts INT NOT NULL DEFAULT 0;
