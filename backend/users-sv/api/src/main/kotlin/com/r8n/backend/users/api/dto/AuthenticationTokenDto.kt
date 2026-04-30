package com.r8n.backend.users.api.dto

data class AuthenticationTokenDto(
    val accessToken: String,
    val expiresInMilliseconds: Long,
)
