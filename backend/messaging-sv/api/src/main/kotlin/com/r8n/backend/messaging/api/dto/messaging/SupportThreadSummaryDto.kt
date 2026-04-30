package com.r8n.backend.messaging.api.dto.messaging

import java.time.Instant
import java.util.UUID

data class SupportThreadSummaryDto(
    val id: UUID,
    val ownerUserId: UUID,
    val createdAt: Instant,
    val lastMessageAt: Instant?,
)
