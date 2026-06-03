-- Optimistic locking for the user-context write aggregates.
-- Hibernate manages this column via @Version; concurrent updates to the same
-- row now fail fast with an OptimisticLockException instead of silently
-- overwriting each other. DEFAULT 0 backfills the rows already in place.

ALTER TABLE users
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE teacher_student_bonds
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
