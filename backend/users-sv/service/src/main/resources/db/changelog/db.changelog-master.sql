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