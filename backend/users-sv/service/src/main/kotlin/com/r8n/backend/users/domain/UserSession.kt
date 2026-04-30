package com.r8n.backend.users.domain

import java.time.Instant
import java.util.UUID

data class UserSession(
    val id: UUID,
    val created: Instant,
    val expires: Instant,
    val ip: String,
    val os: String,
    val userAgent: String,
)
