package com.r8n.backend.users.api.dto

import java.time.Instant
import java.util.UUID

data class UserSearchResultDto(
    val id: UUID,
    val name: String,
    val lastSeenAt: Instant?,
)
