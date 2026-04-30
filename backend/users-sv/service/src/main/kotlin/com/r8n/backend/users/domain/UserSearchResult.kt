package com.r8n.backend.users.domain

import java.time.Instant
import java.util.UUID

data class UserSearchResult(
    val id: UUID,
    val name: String,
    val lastSeenAt: Instant?,
)
