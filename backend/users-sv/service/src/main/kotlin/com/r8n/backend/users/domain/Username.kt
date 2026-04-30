package com.r8n.backend.users.domain

import java.util.UUID

data class Username(
    val id: UUID,
    val name: String,
    val roles: List<String>,
)
