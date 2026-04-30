package com.r8n.backend.users.domain

import com.r8n.backend.users.persistence.RoleEnumPersistence
import java.util.UUID

data class UserWithRoles(
    val id: UUID,
    val name: String,
    val email: String,
    val status: UserStatusEnum,
    val roles: List<RoleEnumPersistence>,
)
