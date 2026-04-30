package com.r8n.backend.messaging.api.dto.messaging

import java.time.Instant
import java.util.UUID

data class DirectMessageDto(
    val id: UUID,
    val conversationId: UUID,
    val authorUserId: UUID,
    val authorDisplayName: String,
    val authorRole: MessageAuthorRoleEnumDto,
    val text: String,
    val createdAt: Instant,
)
