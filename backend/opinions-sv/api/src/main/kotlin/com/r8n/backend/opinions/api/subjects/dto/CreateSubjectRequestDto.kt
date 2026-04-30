package com.r8n.backend.opinions.api.subjects.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

const val SUBJECT_NAME_MAX_LENGTH = 255

data class CreateSubjectRequestDto(
    @field:NotBlank
    @field:Size(max = SUBJECT_NAME_MAX_LENGTH)
    val name: String,
    val primaryReferentId: UUID? = null,
    @field:Size(max = SUBJECT_NAME_MAX_LENGTH)
    val referentName: String?,
    val address: String?,
    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    val latitude: Double?,
    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    val longitude: Double?,
)
