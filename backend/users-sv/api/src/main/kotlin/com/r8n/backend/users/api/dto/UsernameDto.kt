package com.r8n.backend.users.api.dto

import java.util.UUID

data class UsernameDto(
    val id: UUID,
    val name: String,
    val roles: List<RoleEnumDto>,
)
