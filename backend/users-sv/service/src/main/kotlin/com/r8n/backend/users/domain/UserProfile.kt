package com.r8n.backend.users.domain

import java.time.Instant
import java.util.UUID

data class UserProfile(
    val id: UUID,
    val name: String,
    val status: UserStatusEnum,
    val lastSeenAt: Instant?,
    val about: String?,
    val location: String?,
)
