package com.r8n.backend.users.api.dto

data class LoginRequestDto(
    val login: String,
    val password: String,
)
