package com.r8n.backend.users.api.dto

data class UpdateMyPublicProfileRequestDto(
    val name: String,
    val about: String?,
    val location: String?,
)
