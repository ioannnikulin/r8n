package com.r8n.backend.users.persistence

import com.r8n.backend.users.domain.UserStatusEnum
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(schema = "users", name = "users")
class UserPersistence(
    @Id
    @GeneratedValue
    @UuidGenerator
    val id: UUID,
//
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val status: UserStatusEnum,
//
    @Column(nullable = false)
    val statusTimestamp: Instant,
    @Column(nullable = true)
    val passwordHash: String? = null,
)

enum class UserStatusEnum {
    ACTIVE,
    SUSPENDED,
    DELETION_PENDING,
    DELETED,
}