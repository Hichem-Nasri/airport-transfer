-- Forgot-password flow: single-use, short-lived tokens. Only a SHA-256 hash of the token is stored.
CREATE TABLE password_reset_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64)  NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_reset_token_hash ON password_reset_tokens (token_hash);
CREATE INDEX ix_reset_token_user ON password_reset_tokens (user_id);
