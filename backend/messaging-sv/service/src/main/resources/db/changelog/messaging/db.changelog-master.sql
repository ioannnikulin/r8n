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

--changeset iatopchu:V3_create_common_messaging_tables
CREATE TABLE IF NOT EXISTS messaging.conversations (
    id UUID PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    last_message_at TIMESTAMPTZ,
    CONSTRAINT chk_conversations_type
        CHECK (type IN ('SUPPORT', 'DIRECT'))
);
CREATE INDEX IF NOT EXISTS idx_conversations_type_last_message_at
    ON messaging.conversations(type, last_message_at DESC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_conversations_created_by_user_id
    ON messaging.conversations(created_by_user_id);

CREATE TABLE IF NOT EXISTS messaging.conversation_participants (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    participant_role VARCHAR(32) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ,
    last_read_at TIMESTAMPTZ,
    CONSTRAINT fk_conversation_participants_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES messaging.conversations(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_conversation_participants_conversation_user
        UNIQUE (conversation_id, user_id),
    CONSTRAINT chk_conversation_participants_role
        CHECK (participant_role IN ('MEMBER', 'SUPPORT_AGENT'))
);
CREATE INDEX IF NOT EXISTS idx_conversation_participants_user_id
    ON messaging.conversation_participants(user_id);
CREATE INDEX IF NOT EXISTS idx_conversation_participants_conversation_id
    ON messaging.conversation_participants(conversation_id);

CREATE TABLE IF NOT EXISTS messaging.messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    author_user_id UUID NOT NULL,
    author_display_name_snapshot VARCHAR(255) NOT NULL,
    author_role_snapshot VARCHAR(32) NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_messages_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES messaging.conversations(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_messages_author_role_snapshot
        CHECK (author_role_snapshot IN ('USER', 'MODERATOR', 'SUPPORT', 'ADMIN'))
);
CREATE INDEX IF NOT EXISTS idx_messages_conversation_created_at
    ON messaging.messages(conversation_id, created_at);
CREATE INDEX IF NOT EXISTS idx_messages_author_user_id
    ON messaging.messages(author_user_id);

--changeset codex:V4_support_team_read_state
ALTER TABLE messaging.support_messages
    ADD COLUMN IF NOT EXISTS read_by_support_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS read_by_support_user_id UUID;

CREATE INDEX IF NOT EXISTS idx_support_messages_unread_support
    ON messaging.support_messages(thread_id, created_at)
    WHERE author_role = 'USER' AND read_by_support_at IS NULL;

UPDATE messaging.support_messages user_message
SET
    read_by_support_at = (
        SELECT support_message.created_at
        FROM messaging.support_messages support_message
        WHERE support_message.thread_id = user_message.thread_id
          AND support_message.author_role = 'SUPPORT'
          AND support_message.created_at > user_message.created_at
        ORDER BY support_message.created_at ASC
        LIMIT 1
    ),
    read_by_support_user_id = (
        SELECT support_message.author_user_id
        FROM messaging.support_messages support_message
        WHERE support_message.thread_id = user_message.thread_id
          AND support_message.author_role = 'SUPPORT'
          AND support_message.created_at > user_message.created_at
        ORDER BY support_message.created_at ASC
        LIMIT 1
    )
WHERE user_message.author_role = 'USER'
  AND user_message.read_by_support_at IS NULL
  AND EXISTS (
      SELECT 1
      FROM messaging.support_messages support_message
      WHERE support_message.thread_id = user_message.thread_id
        AND support_message.author_role = 'SUPPORT'
        AND support_message.created_at > user_message.created_at
  );

--changeset codex:V5_support_requester_read_state
ALTER TABLE messaging.support_threads
    ADD COLUMN IF NOT EXISTS requester_last_read_at TIMESTAMPTZ;

UPDATE messaging.support_threads thread
SET requester_last_read_at = latest_message.created_at
FROM (
    SELECT thread_id, MAX(created_at) AS created_at
    FROM messaging.support_messages
    GROUP BY thread_id
) latest_message
WHERE thread.id = latest_message.thread_id
  AND thread.requester_last_read_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_support_threads_owner_requester_read
    ON messaging.support_threads(owner_user_id, requester_last_read_at);

CREATE INDEX IF NOT EXISTS idx_support_messages_unread_requester
    ON messaging.support_messages(thread_id, created_at)
    WHERE author_role = 'SUPPORT';
