CREATE TYPE device_platform AS ENUM ('ANDROID', 'IOS');

CREATE TABLE user_device_tokens
(
    id         UUID PRIMARY KEY,
    user_id    UUID REFERENCES users (id) ON DELETE CASCADE NOT NULL,
    token      TEXT UNIQUE                                  NOT NULL,
    platform   device_platform                              NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_device_tokens_user ON user_device_tokens (user_id);