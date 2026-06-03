CREATE TABLE users (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    phone       VARCHAR(20)  NOT NULL,
    birth_date  DATE         NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    status      VARCHAR(30)  NOT NULL,
    created_by  UUID         NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('STUDENT', 'INSTRUCTOR', 'ADMINISTRATOR')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED', 'PENDING_FIRST_ACCESS'))
);

CREATE INDEX ix_users_role_status ON users (role, status);
CREATE INDEX ix_users_created_by ON users (created_by);
