CREATE TABLE workout_executions (
    id            UUID         PRIMARY KEY,
    student_id    UUID         NOT NULL,
    training_id   UUID         NOT NULL,
    started_at    TIMESTAMPTZ  NOT NULL,
    finished_at   TIMESTAMPTZ  NOT NULL,
    notes         VARCHAR(1000),
    registered_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_workout_executions_window CHECK (finished_at >= started_at),
    -- Idempotency (RF-007): one execution per student + training + start instant.
    CONSTRAINT uq_workout_executions_key UNIQUE (student_id, training_id, started_at)
);

CREATE INDEX ix_workout_executions_student ON workout_executions (student_id);
CREATE INDEX ix_workout_executions_training ON workout_executions (training_id);

CREATE TABLE workout_execution_items (
    id           UUID         PRIMARY KEY,
    execution_id UUID         NOT NULL REFERENCES workout_executions (id) ON DELETE CASCADE,
    exercise_id  UUID         NOT NULL,
    position     INTEGER      NOT NULL,
    sets         INTEGER      NOT NULL,
    repetitions  INTEGER      NOT NULL,
    load_kg      NUMERIC(7, 2),
    notes        VARCHAR(500),
    CONSTRAINT ck_workout_execution_items_sets CHECK (sets > 0),
    CONSTRAINT ck_workout_execution_items_reps CHECK (repetitions > 0),
    CONSTRAINT ck_workout_execution_items_load CHECK (load_kg IS NULL OR load_kg >= 0)
);

CREATE INDEX ix_workout_execution_items_execution ON workout_execution_items (execution_id);
