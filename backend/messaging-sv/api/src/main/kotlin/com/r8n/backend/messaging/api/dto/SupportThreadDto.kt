package com.r8n.backend.messaging.api.dto

import java.util.UUID

data class SupportThreadDto(
    val id: UUID,
    val messages: List<String>,
)
