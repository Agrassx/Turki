-- V2: Add timezone column to users table
-- Default to Europe/Moscow since the bot targets Russian-speaking users

ALTER TABLE app.users
    ADD COLUMN IF NOT EXISTS timezone VARCHAR(64) NOT NULL DEFAULT 'Europe/Moscow';
