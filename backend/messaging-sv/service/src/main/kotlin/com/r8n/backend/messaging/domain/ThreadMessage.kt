package com.r8n.backend.messaging.domain

import java.time.Instant
import java.util.UUID

data class ThreadMessage(
    val id: UUID,
    val threadId: UUID,
    val authorUserId: UUID,
    val authorRole: MessageAuthorRoleEnum,
    val text: String,
    val createdAt: Instant,
)