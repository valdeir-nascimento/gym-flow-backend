-- The token-bearing tables (invites, refresh_tokens, password_reset_tokens) were
-- created with token_hash as CHAR(64), which PostgreSQL reports as `bpchar`. The
-- JPA entities map it as String length 64 -> VARCHAR(64), so Hibernate's
-- schema validation (ddl-auto: validate) fails at startup with:
--   "wrong column type ... found [bpchar], but expecting [varchar(64)]".
--
-- Align the column type to VARCHAR(64). The stored hashes are fixed-length hex
-- SHA-256 digests (exactly 64 chars), so rtrim is a no-op safeguard against any
-- CHAR space-padding. Done in a new migration (not by editing V4/V5/V7) to keep
-- Flyway checksums of already-applied migrations intact.

ALTER TABLE invites
    ALTER COLUMN token_hash TYPE VARCHAR(64) USING rtrim(token_hash);

ALTER TABLE refresh_tokens
    ALTER COLUMN token_hash TYPE VARCHAR(64) USING rtrim(token_hash);

ALTER TABLE password_reset_tokens
    ALTER COLUMN token_hash TYPE VARCHAR(64) USING rtrim(token_hash);
