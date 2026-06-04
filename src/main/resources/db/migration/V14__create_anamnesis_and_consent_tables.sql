CREATE TABLE health_data_consents (
    id         UUID        PRIMARY KEY,
    student_id UUID        NOT NULL UNIQUE,
    granted_by UUID        NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE anamnesis_revisions (
    id                   UUID          PRIMARY KEY,
    student_id           UUID          NOT NULL,
    version              INTEGER       NOT NULL,
    weight_kg            NUMERIC(6, 2) NOT NULL,
    height_cm            INTEGER       NOT NULL,
    objectives           VARCHAR(2000),
    conditioning_history VARCHAR(2000),
    injuries             VARCHAR(2000),
    medical_restrictions VARCHAR(2000),
    observations         VARCHAR(2000),
    created_by           UUID          NOT NULL,
    created_at           TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_anamnesis_weight CHECK (weight_kg >= 20 AND weight_kg <= 400),
    CONSTRAINT ck_anamnesis_height CHECK (height_cm >= 50 AND height_cm <= 260),
    -- One revision per version per student (RF-017); the use case assigns the next version.
    CONSTRAINT uq_anamnesis_student_version UNIQUE (student_id, version)
);

CREATE INDEX ix_anamnesis_student ON anamnesis_revisions (student_id);

CREATE TABLE anamnesis_contraindications (
    id           UUID    PRIMARY KEY,
    anamnesis_id UUID    NOT NULL REFERENCES anamnesis_revisions (id) ON DELETE CASCADE,
    exercise_id  UUID    NOT NULL,
    position     INTEGER NOT NULL
);

CREATE INDEX ix_anamnesis_contraindications_revision ON anamnesis_contraindications (anamnesis_id);
