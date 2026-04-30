package com.r8n.backend.opinions.api.access.dto

import java.time.Instant
import java.util.UUID

data class AccessRequestDto(
    val id: UUID,
    val opinionListId: UUID,
    val opinionListName: String,
    val owner: UUID,
    val ownerName: String,
    val requester: UUID,
    val requesterName: String,
    val timestamp: Instant,
    val status: RequestStatusEnumDto,
)
