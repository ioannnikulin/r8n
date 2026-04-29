package com.r8n.backend.opinions.api.opinions.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

const val REJECTION_REASON_MAX_LENGTH = 2000

data class RejectOpinionRequestDto(
    @field:NotBlank
    @field:Size(max = REJECTION_REASON_MAX_LENGTH)
    val reason: String,
)
