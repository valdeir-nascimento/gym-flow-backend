CREATE TABLE exercises (
    id               UUID          PRIMARY KEY,
    name             VARCHAR(150)  NOT NULL,
    muscle_group     VARCHAR(30)   NOT NULL,
    description      VARCHAR(2000),
    equipment        VARCHAR(100),
    difficulty_level VARCHAR(20)   NOT NULL,
    video_url        VARCHAR(500),
    image_url        VARCHAR(500),
    status           VARCHAR(20)   NOT NULL,
    created_by       UUID,
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    version          BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ck_exercises_muscle_group CHECK (muscle_group IN
        ('CHEST', 'BACK', 'SHOULDERS', 'ARMS', 'LEGS', 'GLUTES', 'CORE', 'CARDIO', 'FULL_BODY')),
    CONSTRAINT ck_exercises_difficulty CHECK (difficulty_level IN
        ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT ck_exercises_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- Exercise name is globally unique, case-insensitive (RF-011).
CREATE UNIQUE INDEX ux_exercises_name_lower ON exercises (LOWER(name));

-- Catalog search filters.
CREATE INDEX ix_exercises_muscle_group ON exercises (muscle_group);
CREATE INDEX ix_exercises_difficulty ON exercises (difficulty_level);
