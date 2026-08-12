CREATE TABLE blacklisted_tokens
(
    token TEXT PRIMARY KEY,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);