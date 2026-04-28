package com.r8n.backend.messaging.api.dto

import java.util.UUID

data class CreateDirectThreadRequestDto(
    val recipientUserId: UUID,
    val initialMessage: String,
)