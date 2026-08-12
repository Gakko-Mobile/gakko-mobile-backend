CREATE TABLE blacklisted_tokens
(
    token VARCHAR(100) PRIMARY KEY,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);