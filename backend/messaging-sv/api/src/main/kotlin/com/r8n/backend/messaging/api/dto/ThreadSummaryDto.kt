package com.r8n.backend.messaging.api.dto

import java.time.Instant
import java.util.UUID

data class ThreadSummaryDto(
    val id: UUID,
    val type: ThreadTypeEnumDto,
    val participant: ThreadParticipantDto,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastMessagePreview: String,
    val lastMessageOwn: Boolean,
    val unreadCount: Long,
)