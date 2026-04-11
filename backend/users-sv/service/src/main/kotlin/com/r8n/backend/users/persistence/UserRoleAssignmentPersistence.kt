package com.r8n.backend.users.persistence

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
@Table(schema = "users", name = "users_role_assignments")
class UserRoleAssignmentPersistence(
    @Id
    @GeneratedValue
    @UuidGenerator
    val id: UUID,
//
    @Column(nullable = false)
    val user: UUID,
//
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val role: RoleEnumPersistence,
//
    @Column(nullable = false)
    val grantedBy: UUID,
//
    @Column(nullable = false)
    val timestamp: Instant,
)

enum class RoleEnumPersistence {
    ADMIN,
    MODERATOR,
    AI_MODERATOR,
    USER,
}