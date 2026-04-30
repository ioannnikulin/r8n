package com.r8n.backend.opinions.api.opinions.dto

import java.util.UUID

data class ReferentDto(
    val id: UUID,
    val name: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
)
