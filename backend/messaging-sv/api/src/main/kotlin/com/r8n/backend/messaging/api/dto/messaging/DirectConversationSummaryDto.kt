package com.r8n.backend.messaging.api.dto.messaging

import java.time.Instant
import java.util.UUID

data class DirectConversationSummaryDto(
    val id: UUID,
    val participantUserId: UUID,
    val participantDisplayName: String,
    val createdAt: Instant,
    val lastMessageAt: Instant?,
    val lastMessageText: String?,
    val unreadCount: Long,
)
