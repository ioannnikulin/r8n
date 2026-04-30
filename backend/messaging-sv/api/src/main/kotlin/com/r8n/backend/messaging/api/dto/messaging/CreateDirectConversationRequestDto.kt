package com.r8n.backend.messaging.api.dto.messaging

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateDirectConversationRequestDto(
    val recipientUserId: UUID,
    @field:NotBlank
    @field:Size(max = SUPPORT_MESSAGE_TEXT_MAX_LENGTH)
    val initialMessage: String,
)
