--liquibase formatted sql

--changeset inikulin:V1_create_tables
CREATE TABLE users.users (
    id UUID PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    status_timestamp TIMESTAMPTZ NOT NULL,
    password_hash VARCHAR(255)
);

CREATE TABLE users.pii (
    user_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(255),
    CONSTRAINT fk_pii_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE
);

CREATE TABLE users.sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    created TIMESTAMPTZ NOT NULL,
    expires TIMESTAMPTZ NOT NULL,
    ip VARCHAR(45) NOT NULL,
    user_agent TEXT NOT NULL,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE
);
CREATE INDEX idx_sessions_user_id ON users.sessions(user_id);

CREATE TABLE users.consents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(64) NOT NULL,
    accepted TIMESTAMPTZ NOT NULL,
    session UUID NOT NULL,
    CONSTRAINT fk_consent_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_consent_session FOREIGN KEY (session) REFERENCES users.sessions(id) ON DELETE CASCADE
);
CREATE INDEX idx_consents_user_id ON users.consents(user_id);
CREATE INDEX idx_consents_session ON users.consents(session);

CREATE TABLE users.users_role_assignments (
    id UUID PRIMARY KEY,
    "user" UUID NOT NULL,
    role VARCHAR(32) NOT NULL,
    granted_by UUID NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_role_assignment_user FOREIGN KEY ("user") REFERENCES users.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_assignment_granter FOREIGN KEY (granted_by) REFERENCES users.users(id) ON DELETE CASCADE
);
CREATE INDEX idx_users_role_assignments_user_id ON users.users_role_assignments("user");

--changeset inikulin:V2_seed_data context:local,test
-- password is '1234' hashed with BCrypt
INSERT INTO users.users (id, status, status_timestamp, password_hash)
VALUES ('00000000-0000-0000-0000-000000000000', 'ACTIVE', '2024-01-01T12:00:00Z', '$2a$12$lxo9e8RbWABER4/mkU./s.njgArpJleAB9Vdq7C7rlNWIRYEw0Oym');
INSERT INTO users.pii (user_id, name, email, phone)
VALUES ('00000000-0000-0000-0000-000000000000', 'Test Testsson', 'test@test.test', '123-456-7890');
INSERT INTO users.sessions(id, user_id, created, expires, ip, user_agent)
VALUES ('01010101-0101-0101-0101-010101010101'
, '00000000-0000-0000-0000-000000000000'
, '2024-01-01T12:00:00Z'
, '2024-01-02T12:00:00Z'
,'127.0.0.1'
, 'Test User Agent'
);
INSERT INTO users.consents(id, user_id, type, accepted, session)
VALUES ('02020202-0202-0202-0202-020202020202'
, '00000000-0000-0000-0000-000000000000'
, 'PRIVACY_POLICY'
, '2024-01-01T12:00:00Z'
, '01010101-0101-0101-0101-010101010101'
);
INSERT INTO users.users (id, status, status_timestamp, password_hash)
VALUES ('10101010-1010-1010-1010-101010101010', 'ACTIVE', '2024-01-01T12:00:00Z', '$2a$10$f3pE67YFqJz6Yy7p0vW2ueZ9u0Yk4H9/fS7M8p2k5hWz/96K0V/q');
INSERT INTO users.pii (user_id, name, email, phone)
VALUES ('10101010-1010-1010-1010-101010101010', 'coffee expert Bernard', 'bernard@coffee.com', '123-456-7890');

--changeset junie:V3_refresh_token_rotation
CREATE TABLE users.refresh_tokens (
    id UUID PRIMARY KEY,
    token_id UUID NOT NULL,
    user_id UUID NOT NULL,
    parent_id UUID,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_token_parent FOREIGN KEY (parent_id) REFERENCES users.refresh_tokens(id) ON DELETE CASCADE
);
CREATE INDEX idx_refresh_tokens_user_id ON users.refresh_tokens(user_id);
CREATE UNIQUE INDEX idx_refresh_tokens_token_id ON users.refresh_tokens(token_id);

--changeset ditabisko:V3_seed_additional_users context:local,test
-- password is '1234' hashed with BCrypt
INSERT INTO users.users (id, status, status_timestamp, password_hash)
VALUES ('20202020-2020-2020-2020-202020202020', 'ACTIVE', '2024-01-01T12:00:00Z', '$2a$12$lxo9e8RbWABER4/mkU./s.njgArpJleAB9Vdq7C7rlNWIRYEw0Oym');
INSERT INTO users.pii (user_id, name, email, phone)
VALUES ('20202020-2020-2020-2020-202020202020', 'Anna Müller', 'anna@r8n.test', null);

INSERT INTO users.users (id, status, status_timestamp, password_hash)
VALUES ('30303030-3030-3030-3030-303030303030', 'ACTIVE', '2024-01-01T12:00:00Z', '$2a$12$lxo9e8RbWABER4/mkU./s.njgArpJleAB9Vdq7C7rlNWIRYEw0Oym');
INSERT INTO users.pii (user_id, name, email, phone)
VALUES ('30303030-3030-3030-3030-303030303030', 'Lena Koch', 'lena@r8n.test', null);

INSERT INTO users.users (id, status, status_timestamp, password_hash)
VALUES ('40404040-4040-4040-4040-404040404040', 'ACTIVE', '2024-01-01T12:00:00Z', '$2a$12$lxo9e8RbWABER4/mkU./s.njgArpJleAB9Vdq7C7rlNWIRYEw0Oym');
INSERT INTO users.pii (user_id, name, email, phone)
VALUES ('40404040-4040-4040-4040-404040404040', 'Max Weber', 'max@r8n.test', null);

INSERT INTO users.users (id, status, status_timestamp, password_hash)
VALUES ('50505050-5050-5050-5050-505050505050', 'ACTIVE', '2024-01-01T12:00:00Z', '$2a$12$lxo9e8RbWABER4/mkU./s.njgArpJleAB9Vdq7C7rlNWIRYEw0Oym');
INSERT INTO users.pii (user_id, name, email, phone)
VALUES ('50505050-5050-5050-5050-505050505050', 'Sofia Bauer', 'sofia@r8n.test', null);

INSERT INTO users.users (id, status, status_timestamp, password_hash)
VALUES ('60606060-6060-6060-6060-606060606060', 'ACTIVE', '2024-01-01T12:00:00Z', '$2a$12$lxo9e8RbWABER4/mkU./s.njgArpJleAB9Vdq7C7rlNWIRYEw0Oym');
INSERT INTO users.pii (user_id, name, email, phone)
VALUES ('60606060-6060-6060-6060-606060606060', 'Jonas Braun', 'jonas@r8n.test', null);

--changeset inikulin:V4_user_fields
ALTER TABLE users.pii ADD COLUMN about TEXT;
ALTER TABLE users.pii ADD COLUMN location VARCHAR(255);
UPDATE users.pii SET location = 'Berlin, Germany' WHERE user_id = '00000000-0000-0000-0000-000000000000';
UPDATE users.pii SET about = 'I am a coffee expert' WHERE user_id = '00000000-0000-0000-0000-000000000000';
UPDATE users.pii SET location = 'Munich, Germany' WHERE user_id = '10101010-1010-1010-1010-101010101010';
UPDATE users.pii SET about = 'I am a bratwurst expert' WHERE user_id = '10101010-1010-1010-1010-101010101010';
CREATE UNIQUE INDEX idx_user_name ON users.pii(name);

--changeset mkulikov:V5_profile_avatars
CREATE TABLE users.profile_avatars (
    user_id UUID PRIMARY KEY,
    storage_backend VARCHAR(32) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    file_size BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_profile_avatar_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE
);

--changeset codex:V6_user_last_seen_at
ALTER TABLE users.users ADD COLUMN last_seen_at TIMESTAMPTZ;

--changeset iatopchu:V7_add_session_os
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'users' AND table_name = 'sessions' AND column_name = 'os';
ALTER TABLE users.sessions
    ADD COLUMN os VARCHAR(255) NOT NULL DEFAULT 'Unknown';

--changeset inikulin:V8_api_keys
CREATE TABLE users.api_keys (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    key_identifier VARCHAR(255) NOT NULL,
    key_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_api_key_user FOREIGN KEY (user_id) REFERENCES users.users(id) ON DELETE CASCADE,
    CONSTRAINT uk_api_keys_key_identifier UNIQUE (key_identifier)
);
CREATE INDEX idx_api_keys_user_id ON users.api_keys(user_id);
CREATE INDEX idx_api_keys_key_identifier ON users.api_keys(key_identifier);

--changeset inikulin:V9_preseed_api_keys
-- raw key: 1234, identifier: test-key -> full key: r8n_test-key_1234
INSERT INTO users.api_keys (id, user_id, key_identifier, key_hash, name, created_at)
VALUES ('00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 'test-key', '$2a$12$lxo9e8RbWABER4/mkU./s.njgArpJleAB9Vdq7C7rlNWIRYEw0Oym', 'Test Key', '2024-01-01T12:00:00Z');

--changeset codex:V10_normalized_email_unique_index
UPDATE users.pii SET email = lower(trim(email));
CREATE UNIQUE INDEX idx_pii_normalized_email ON users.pii (lower(email));

--changeset ditabisko:V10_seed_test_admin_role context:local,test
INSERT INTO users.users_role_assignments (id, "user", role, granted_by, timestamp)
VALUES ('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', '00000000-0000-0000-0000-000000000000', 'ADMIN', '00000000-0000-0000-0000-000000000000', '2024-01-01T12:00:00Z')
ON CONFLICT DO NOTHING;

--changeset ditabisko:V11_seed_lena_moderator_role context:local,test
INSERT INTO users.users_role_assignments (id, "user", role, granted_by, timestamp)
VALUES ('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', '30303030-3030-3030-3030-303030303030', 'MODERATOR', '00000000-0000-0000-0000-000000000000', '2024-01-01T12:00:00Z')
ON CONFLICT DO NOTHING;

--changeset iatopchu:V11_seed_support_role context:local,test
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM users.users_role_assignments WHERE id = '77777777-7777-7777-7777-777777777777';
INSERT INTO users.users_role_assignments (id, "user", role, granted_by, timestamp)
VALUES (
    '77777777-7777-7777-7777-777777777777',
    '10101010-1010-1010-1010-101010101010',
    'SUPPORT',
    '10101010-1010-1010-1010-101010101010',
    '2024-01-01T12:00:00Z'
);

--changeset ditabisko:V12_unique_user_role_constraint
ALTER TABLE users.users_role_assignments
    ADD CONSTRAINT uq_user_role UNIQUE ("user", role);

--changeset ditabisko:V13_upgrade_test_user_to_admin context:local,test
UPDATE users.users_role_assignments
SET role = 'ADMIN'
WHERE id = 'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0'
  AND role <> 'ADMIN';
