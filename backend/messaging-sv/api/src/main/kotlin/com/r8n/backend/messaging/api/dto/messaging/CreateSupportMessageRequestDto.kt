package com.r8n.backend.messaging.api.dto.messaging

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateSupportMessageRequestDto(
    @field:NotBlank
    @field:Size(max = SUPPORT_MESSAGE_TEXT_MAX_LENGTH)
    val text: String,
)
