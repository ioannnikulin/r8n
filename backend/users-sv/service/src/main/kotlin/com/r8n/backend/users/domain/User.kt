package com.r8n.backend.users.domain

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val name: String,
    val email: String,
    val phone: String?,
    val about: String?,
    val location: String?,
    val status: UserStatusEnum,
    val statusTimestamp: Instant,
    val consents: List<Consent>,
)

enum class UserStatusEnum {
    ACTIVE,
    SUSPENDED,
    DELETION_PENDING,
    DELETED,
}
