--liquibase formatted sql

--changeset r8n:V1_create_messaging_schema
CREATE SCHEMA IF NOT EXISTS messaging;

--changeset r8n:V2_create_messaging_tables
CREATE TABLE messaging.threads (
    id UUID PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    requester_user_id UUID NOT NULL,
    recipient_user_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    requester_read_at TIMESTAMPTZ,
    recipient_read_at TIMESTAMPTZ,
    support_read_at TIMESTAMPTZ,
    CONSTRAINT chk_threads_type CHECK (type IN ('SUPPORT', 'DIRECT')),
    CONSTRAINT chk_threads_participants_by_type CHECK (
        (
            type = 'SUPPORT'
            AND recipient_user_id IS NULL
            AND recipient_read_at IS NULL
        )
        OR
        (
            type = 'DIRECT'
            AND recipient_user_id IS NOT NULL
            AND support_read_at IS NULL
            AND requester_user_id <> recipient_user_id
        )
    )
);
CREATE INDEX idx_threads_requester_user_id ON messaging.threads(requester_user_id);
CREATE INDEX idx_threads_recipient_user_id ON messaging.threads(recipient_user_id);
CREATE INDEX idx_threads_type_updated_at ON messaging.threads(type, updated_at DESC);
CREATE INDEX idx_threads_requester_updated_at ON messaging.threads(requester_user_id, updated_at DESC);
CREATE INDEX idx_threads_recipient_updated_at ON messaging.threads(recipient_user_id, updated_at DESC);
CREATE UNIQUE INDEX uq_threads_direct_participants
    ON messaging.threads (
        LEAST(requester_user_id, recipient_user_id),
        GREATEST(requester_user_id, recipient_user_id)
    )
    WHERE type = 'DIRECT';

CREATE TABLE messaging.messages (
    id UUID PRIMARY KEY,
    thread_id UUID NOT NULL,
    author_user_id UUID NOT NULL,
    author_role VARCHAR(32) NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_messages_thread FOREIGN KEY (thread_id) REFERENCES messaging.threads(id) ON DELETE CASCADE,
    CONSTRAINT chk_messages_author_role CHECK (author_role IN ('USER', 'SUPPORT')),
    CONSTRAINT chk_messages_text_not_blank CHECK (btrim(text) <> '')
);
CREATE INDEX idx_messages_thread_created_at ON messaging.messages(thread_id, created_at);
CREATE INDEX idx_messages_author_user_id ON messaging.messages(author_user_id);
