CREATE TABLE login_attempts (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    attempted_at  TIMESTAMPTZ  NOT NULL,
    success       BOOLEAN      NOT NULL,
    ip_address    VARCHAR(45)
);

CREATE INDEX ix_login_attempts_email_time ON login_attempts (email, attempted_at DESC);
