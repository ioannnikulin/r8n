package com.r8n.backend.messaging.api.dto.messaging

import java.time.Instant
import java.util.UUID

data class SupportMessageDto(
    val id: UUID,
    val threadId: UUID,
    val authorUserId: UUID,
    val authorDisplayName: String,
    val authorRole: SupportParticipantRoleEnumDto,
    val text: String,
    val createdAt: Instant,
)
