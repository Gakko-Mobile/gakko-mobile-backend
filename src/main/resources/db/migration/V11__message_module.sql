CREATE TABLE chat_threads
(
    id          UUID PRIMARY KEY,
    subject     VARCHAR(255)                                       NOT NULL,
    thread_type VARCHAR(50)                                        NOT NULL, -- DIRECT, GROUP, OFFICIAL
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE chat_thread_participants
(
    id           UUID PRIMARY KEY,
    thread_id    UUID REFERENCES chat_threads (id) ON DELETE CASCADE NOT NULL,
    user_id      UUID REFERENCES users (id) ON DELETE CASCADE        NOT NULL,
    last_read_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_chat_thread_participant UNIQUE (thread_id, user_id)
);

CREATE INDEX idx_chat_thread_participants_user ON chat_thread_participants (user_id);

CREATE TABLE chat_messages
(
    id                    UUID PRIMARY KEY,
    thread_id             UUID REFERENCES chat_threads (id) ON DELETE CASCADE NOT NULL,
    sender_id             UUID REFERENCES users (id) ON DELETE SET NULL,
    content               TEXT                                               NOT NULL,
    is_urgent             BOOLEAN                  DEFAULT FALSE             NOT NULL,
    is_buffered           BOOLEAN                  DEFAULT FALSE             NOT NULL,
    scheduled_delivery_at TIMESTAMP WITH TIME ZONE,
    status                VARCHAR(50)                                        NOT NULL, -- DELIVERED, BUFFERED, READ
    created_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_chat_messages_thread_created ON chat_messages (thread_id, created_at DESC);
CREATE INDEX idx_chat_messages_buffered_delivery ON chat_messages (scheduled_delivery_at) WHERE is_buffered = TRUE;

ALTER TABLE user_notification_settings
    ADD COLUMN communication VARCHAR(50) DEFAULT 'PUSH_AND_INAPP' NOT NULL;
