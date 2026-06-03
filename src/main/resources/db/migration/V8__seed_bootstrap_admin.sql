-- Bootstraps a single ADMINISTRATOR for first-time setup.
-- The invite token is intentionally static so it can be consumed via:
--   POST /api/v1/auth/invites/WS_FITNESS_BOOTSTRAP_ADMIN_2026/consume
-- IMPORTANT: remove or replace this migration before deploying to production.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (id, name, email, phone, birth_date, role, status, created_at, updated_at)
SELECT
    '11111111-1111-1111-1111-111111111111'::UUID,
    'Administrador WS Fitness',
    'admin@wsfitness.local',
    '+5511999990000',
    '1990-01-01'::DATE,
    'ADMINISTRATOR',
    'PENDING_FIRST_ACCESS',
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@wsfitness.local');

INSERT INTO user_credentials (user_id, email, password_hash, password_updated_at, role, status)
SELECT
    '11111111-1111-1111-1111-111111111111'::UUID,
    'admin@wsfitness.local',
    NULL,
    NULL,
    'ADMINISTRATOR',
    'PENDING_FIRST_ACCESS'
WHERE NOT EXISTS (SELECT 1 FROM user_credentials WHERE email = 'admin@wsfitness.local');

INSERT INTO invites (id, user_id, token_hash, expires_at, consumed_at, created_at)
SELECT
    '22222222-2222-2222-2222-222222222222'::UUID,
    '11111111-1111-1111-1111-111111111111'::UUID,
    encode(digest('WS_FITNESS_BOOTSTRAP_ADMIN_2026', 'sha256'), 'hex'),
    NOW() + INTERVAL '365 days',
    NULL,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM invites
    WHERE token_hash = encode(digest('WS_FITNESS_BOOTSTRAP_ADMIN_2026', 'sha256'), 'hex')
);
