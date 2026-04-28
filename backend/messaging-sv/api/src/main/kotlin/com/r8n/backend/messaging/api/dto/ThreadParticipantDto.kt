package com.r8n.backend.messaging.api.dto

import java.util.UUID

data class ThreadParticipantDto(
    val userId: UUID?,
    val name: String,
    val role: MessageAuthorRoleEnumDto,
)