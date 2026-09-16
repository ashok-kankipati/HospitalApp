-- Run once in the HospitalApp PostgreSQL database.
-- Replace the email and password below before running (8-72 UTF-8 bytes).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (username, email, password, role, is_active, created_at)
VALUES ('hospital_admin', 'admin@example.com',
        crypt('REPLACE_WITH_A_STRONG_PASSWORD', gen_salt('bf', 12)),
        'Admin', true, CURRENT_TIMESTAMP::text)
ON CONFLICT DO NOTHING
RETURNING id, username, email, role;

-- A returned row confirms creation. No row means username/email already exists;
-- this script never overwrites an existing account.
