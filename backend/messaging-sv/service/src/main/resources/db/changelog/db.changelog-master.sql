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

--changeset r8n:V3_seed_anna_messaging context:local,test
INSERT INTO messaging.threads (
    id,
    type,
    requester_user_id,
    recipient_user_id,
    created_at,
    updated_at,
    requester_read_at,
    recipient_read_at,
    support_read_at
)
VALUES
    (
        '70000000-0000-0000-0000-000000000001',
        'SUPPORT',
        '20202020-2020-2020-2020-202020202020',
        NULL,
        '2026-04-20T09:00:00Z',
        '2026-04-20T09:12:00Z',
        '2026-04-20T09:06:00Z',
        NULL,
        '2026-04-20T09:12:00Z'
    ),
    (
        '70000000-0000-0000-0000-000000000002',
        'DIRECT',
        '20202020-2020-2020-2020-202020202020',
        '00000000-0000-0000-0000-000000000000',
        '2026-04-21T10:00:00Z',
        '2026-04-21T10:15:00Z',
        '2026-04-21T10:10:30Z',
        '2026-04-21T10:16:00Z',
        NULL
    );

INSERT INTO messaging.messages (
    id,
    thread_id,
    author_user_id,
    author_role,
    text,
    created_at
)
VALUES
    (
        '71000000-0000-0000-0000-000000000001',
        '70000000-0000-0000-0000-000000000001',
        '20202020-2020-2020-2020-202020202020',
        'USER',
        'Hi, I need help updating my profile visibility.',
        '2026-04-20T09:00:00Z'
    ),
    (
        '71000000-0000-0000-0000-000000000002',
        '70000000-0000-0000-0000-000000000001',
        '99999999-9999-9999-9999-999999999999',
        'SUPPORT',
        'I can help with that. Which part should be private?',
        '2026-04-20T09:05:00Z'
    ),
    (
        '71000000-0000-0000-0000-000000000003',
        '70000000-0000-0000-0000-000000000001',
        '99999999-9999-9999-9999-999999999999',
        'SUPPORT',
        'I marked the account for review and will follow up here.',
        '2026-04-20T09:12:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000001',
        '70000000-0000-0000-0000-000000000002',
        '20202020-2020-2020-2020-202020202020',
        'USER',
        'Hi Test, can you review my new list?',
        '2026-04-21T10:00:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000002',
        '70000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000000',
        'USER',
        'Sure, send it over.',
        '2026-04-21T10:04:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000003',
        '70000000-0000-0000-0000-000000000002',
        '20202020-2020-2020-2020-202020202020',
        'USER',
        'Thanks, I added the cafes from Berlin.',
        '2026-04-21T10:10:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000004',
        '70000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000000',
        'USER',
        'Looks good. I left one note.',
        '2026-04-21T10:15:00Z'
    );

--changeset r8n:V4_seed_anna_messaging_pagination context:local,test
INSERT INTO messaging.threads (
    id,
    type,
    requester_user_id,
    recipient_user_id,
    created_at,
    updated_at,
    requester_read_at,
    recipient_read_at,
    support_read_at
)
VALUES
    (
        '70000000-0000-0000-0000-000000000003',
        'SUPPORT',
        '20202020-2020-2020-2020-202020202020',
        NULL,
        '2026-04-22T08:30:00Z',
        '2026-04-22T08:30:00Z',
        '2026-04-22T08:30:00Z',
        NULL,
        NULL
    );

INSERT INTO messaging.messages (
    id,
    thread_id,
    author_user_id,
    author_role,
    text,
    created_at
)
VALUES
    (
        '73000000-0000-0000-0000-000000000001',
        '70000000-0000-0000-0000-000000000003',
        '20202020-2020-2020-2020-202020202020',
        'USER',
        'One more support question for pagination testing.',
        '2026-04-22T08:30:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000005',
        '70000000-0000-0000-0000-000000000002',
        '20202020-2020-2020-2020-202020202020',
        'USER',
        'Pagination note 1 from Anna.',
        '2026-04-21T10:20:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000006',
        '70000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000000',
        'USER',
        'Pagination reply 1 from Test.',
        '2026-04-21T10:25:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000007',
        '70000000-0000-0000-0000-000000000002',
        '20202020-2020-2020-2020-202020202020',
        'USER',
        'Pagination note 2 from Anna.',
        '2026-04-21T10:30:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000008',
        '70000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000000',
        'USER',
        'Pagination reply 2 from Test.',
        '2026-04-21T10:35:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000009',
        '70000000-0000-0000-0000-000000000002',
        '20202020-2020-2020-2020-202020202020',
        'USER',
        'Pagination note 3 from Anna.',
        '2026-04-21T10:40:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000010',
        '70000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000000',
        'USER',
        'Pagination reply 3 from Test.',
        '2026-04-21T10:45:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000011',
        '70000000-0000-0000-0000-000000000002',
        '20202020-2020-2020-2020-202020202020',
        'USER',
        'Pagination note 4 from Anna.',
        '2026-04-21T10:50:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000012',
        '70000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000000',
        'USER',
        'Pagination reply 4 from Test.',
        '2026-04-21T10:55:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000013',
        '70000000-0000-0000-0000-000000000002',
        '20202020-2020-2020-2020-202020202020',
        'USER',
        'Pagination note 5 from Anna.',
        '2026-04-21T11:00:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000014',
        '70000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000000',
        'USER',
        'Pagination reply 5 from Test.',
        '2026-04-21T11:05:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000015',
        '70000000-0000-0000-0000-000000000002',
        '20202020-2020-2020-2020-202020202020',
        'USER',
        'Pagination note 6 from Anna.',
        '2026-04-21T11:10:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000016',
        '70000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000000',
        'USER',
        'Pagination reply 6 from Test.',
        '2026-04-21T11:15:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000017',
        '70000000-0000-0000-0000-000000000002',
        '20202020-2020-2020-2020-202020202020',
        'USER',
        'Pagination note 7 from Anna.',
        '2026-04-21T11:20:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000018',
        '70000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000000',
        'USER',
        'Pagination reply 7 from Test.',
        '2026-04-21T11:25:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000019',
        '70000000-0000-0000-0000-000000000002',
        '20202020-2020-2020-2020-202020202020',
        'USER',
        'Pagination note 8 from Anna.',
        '2026-04-21T11:30:00Z'
    ),
    (
        '72000000-0000-0000-0000-000000000020',
        '70000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000000',
        'USER',
        'Pagination reply 8 from Test.',
        '2026-04-21T11:35:00Z'
    );

UPDATE messaging.threads
SET updated_at = '2026-04-21T11:35:00Z',
    requester_read_at = '2026-04-21T11:30:30Z',
    recipient_read_at = '2026-04-21T11:36:00Z'
WHERE id = '70000000-0000-0000-0000-000000000002';
