CREATE TABLE trainings (
    id            UUID         PRIMARY KEY,
    student_id    UUID         NOT NULL,
    instructor_id UUID         NOT NULL,
    name          VARCHAR(150) NOT NULL,
    objective     VARCHAR(500),
    start_date    DATE         NOT NULL,
    end_date      DATE,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_trainings_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_trainings_period CHECK (end_date IS NULL OR end_date >= start_date)
);

-- Overlap check (RF-004) targets a student's ACTIVE trainings.
CREATE INDEX ix_trainings_student_active ON trainings (student_id) WHERE status = 'ACTIVE';
CREATE INDEX ix_trainings_instructor ON trainings (instructor_id);

CREATE TABLE training_items (
    id           UUID         PRIMARY KEY,
    training_id  UUID         NOT NULL REFERENCES trainings (id) ON DELETE CASCADE,
    exercise_id  UUID         NOT NULL,
    position     INTEGER      NOT NULL,
    sets         INTEGER      NOT NULL,
    repetitions  INTEGER      NOT NULL,
    load_kg      NUMERIC(7, 2),
    rest_seconds INTEGER      NOT NULL,
    CONSTRAINT ck_training_items_sets CHECK (sets > 0),
    CONSTRAINT ck_training_items_reps CHECK (repetitions > 0),
    CONSTRAINT ck_training_items_rest CHECK (rest_seconds >= 0),
    CONSTRAINT ck_training_items_load CHECK (load_kg IS NULL OR load_kg >= 0)
);

CREATE INDEX ix_training_items_training ON training_items (training_id);
