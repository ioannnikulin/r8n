package com.r8n.backend.users.service

data class RegistrationAuditContext(
    val ip: String,
    val userAgent: String,
    val operatingSystem: String,
)

data class LoginAuditContext(
    val ip: String,
    val userAgent: String,
    val operatingSystem: String,
)
