--liquibase formatted sql

--changeset iatopchu:V1_create_support_messaging_tables
CREATE SCHEMA IF NOT EXISTS messaging;

CREATE TABLE IF NOT EXISTS messaging.support_threads (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_support_threads_owner_user_id
    ON messaging.support_threads(owner_user_id);

CREATE TABLE IF NOT EXISTS messaging.support_messages (
    id UUID PRIMARY KEY,
    thread_id UUID NOT NULL,
    author_user_id UUID NOT NULL,
    author_role VARCHAR(32) NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_support_messages_thread
        FOREIGN KEY (thread_id)
        REFERENCES messaging.support_threads(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_support_messages_author_role
        CHECK (author_role IN ('USER', 'SUPPORT'))
);
CREATE INDEX IF NOT EXISTS idx_support_messages_thread_id
    ON messaging.support_messages(thread_id);
CREATE INDEX IF NOT EXISTS idx_support_messages_created_at
    ON messaging.support_messages(created_at);

--changeset iatopchu:V2_seed_support_messaging context:local,test
INSERT INTO messaging.support_threads (id, owner_user_id)
VALUES
    (
        '80000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000000'
    ),
    (
        '80000000-0000-0000-0000-000000000002',
        '10101010-1010-1010-1010-101010101010'
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO messaging.support_messages (id, thread_id, author_user_id, author_role, text, created_at)
VALUES
    (
        '81000000-0000-0000-0000-000000000001',
        '80000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000000',
        'USER',
        'I need help with a review dispute.',
        '2024-02-01T09:30:00Z'
    ),
    (
        '81000000-0000-0000-0000-000000000002',
        '80000000-0000-0000-0000-000000000001',
        '10101010-1010-1010-1010-101010101010',
        'SUPPORT',
        'Thanks, we will check the moderation context.',
        '2024-02-01T09:35:00Z'
    ),
    (
        '81000000-0000-0000-0000-000000000003',
        '80000000-0000-0000-0000-000000000002',
        '10101010-1010-1010-1010-101010101010',
        'SUPPORT',
        'Opening an internal support follow-up.',
        '2024-02-01T10:00:00Z'
    )
ON CONFLICT (id) DO NOTHING;
