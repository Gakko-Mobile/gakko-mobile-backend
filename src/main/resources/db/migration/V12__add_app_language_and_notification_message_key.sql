ALTER TABLE users
    ADD COLUMN app_language VARCHAR(2) NOT NULL DEFAULT 'PL'; -- ISO 639-1

ALTER TABLE user_notifications
    ADD COLUMN message_key VARCHAR(100);
