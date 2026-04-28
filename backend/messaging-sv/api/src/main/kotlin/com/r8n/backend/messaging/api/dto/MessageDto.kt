package com.r8n.backend.messaging.api.dto

import java.time.Instant
import java.util.UUID

data class MessageDto(
    val id: UUID,
    val threadId: UUID,
    val authorUserId: UUID,
    val authorName: String,
    val authorRole: MessageAuthorRoleEnumDto,
    val own: Boolean,
    val text: String,
    val createdAt: Instant,
)