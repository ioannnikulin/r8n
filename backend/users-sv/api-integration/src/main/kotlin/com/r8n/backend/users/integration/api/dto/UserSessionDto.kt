package com.r8n.backend.users.integration.api.dto

import java.time.Instant
import java.util.UUID

data class UserSessionDto(
    val id: UUID,
    val created: Instant,
    val expires: Instant,
    val ip: String,
    val os: String,
    val userAgent: String,
)
