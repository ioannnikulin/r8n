package com.r8n.backend.opinions.api.referents.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateReferentRequestDto(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,
    @field:Size(max = 255)
    val address: String?,
    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    val latitude: Double?,
    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    val longitude: Double?,
)
