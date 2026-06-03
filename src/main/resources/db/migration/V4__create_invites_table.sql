CREATE TABLE invites (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id),
    token_hash  CHAR(64)     NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    consumed_at TIMESTAMPTZ  NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_invites_token_hash UNIQUE (token_hash)
);

CREATE INDEX ix_invites_user_active
    ON invites (user_id, created_at DESC)
    WHERE consumed_at IS NULL;
