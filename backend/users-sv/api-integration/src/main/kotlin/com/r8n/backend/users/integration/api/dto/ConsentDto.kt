package com.r8n.backend.users.integration.api.dto

import java.time.Instant

data class ConsentDto(
    val type: String,
    val accepted: Instant,
    val session: UserSessionDto,
)
