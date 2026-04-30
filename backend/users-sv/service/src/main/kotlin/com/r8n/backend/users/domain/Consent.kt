package com.r8n.backend.users.domain

import java.time.Instant
import java.util.UUID

data class Consent(
    val id: UUID,
    val type: String,
    val accepted: Instant,
    val session: UserSession,
)
