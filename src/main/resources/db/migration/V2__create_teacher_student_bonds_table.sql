CREATE TABLE teacher_student_bonds (
    id            UUID         PRIMARY KEY,
    student_id    UUID         NOT NULL REFERENCES users (id),
    instructor_id UUID         NOT NULL REFERENCES users (id),
    started_at    TIMESTAMPTZ  NOT NULL,
    ended_at      TIMESTAMPTZ  NULL,
    created_by    UUID         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

-- One active instructor per student.
CREATE UNIQUE INDEX ux_bonds_active_student
    ON teacher_student_bonds (student_id)
    WHERE ended_at IS NULL;

CREATE INDEX ix_bonds_active_instructor
    ON teacher_student_bonds (instructor_id)
    WHERE ended_at IS NULL;

CREATE INDEX ix_bonds_student_history
    ON teacher_student_bonds (student_id, started_at DESC);
