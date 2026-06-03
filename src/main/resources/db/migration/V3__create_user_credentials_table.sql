CREATE TABLE user_credentials (
    user_id              UUID         PRIMARY KEY REFERENCES users (id),
    email                VARCHAR(255) NOT NULL,
    password_hash        VARCHAR(72)  NULL,
    password_updated_at  TIMESTAMPTZ  NULL,
    role                 VARCHAR(20)  NOT NULL,
    status               VARCHAR(30)  NOT NULL,
    CONSTRAINT uk_user_credentials_email UNIQUE (email)
);
