package com.r8n.backend.users.service

data class AuthenticationTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInMilliseconds: Long,
)
