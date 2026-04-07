package com.r8n.backend.users.api.dto

import java.time.Instant
import java.util.UUID

data class UserDto(
    val id: UUID,
    val name: String,
    val email: String,
    val status: UserStatusEnumDto,
    val statusTimestamp: Instant,
    val consents: List<ConsentDto>,
)