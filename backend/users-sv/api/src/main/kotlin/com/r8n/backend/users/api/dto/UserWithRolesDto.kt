package com.r8n.backend.users.api.dto

import java.util.UUID

data class UserWithRolesDto(
    val id: UUID,
    val name: String,
    val email: String,
    val status: UserStatusEnumDto,
    val isModerator: Boolean,
    val isSupport: Boolean,
    val isAdmin: Boolean,
)
