package com.r8n.backend.users.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(schema = "users", name = "refresh_tokens")
class RefreshTokenPersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null,
//
    @Column(nullable = false)
    val tokenId: UUID,
//
    @Column(nullable = false)
    val userId: UUID,
//
    @Column(name = "parent_id")
    val parentId: UUID? = null,
//
    @Column(nullable = false)
    val issuedAt: Instant,
//
    @Column(nullable = false)
    val expiresAt: Instant,
//
    @Column(nullable = false)
    var revoked: Boolean = false,
//
    @Column(nullable = false)
    var used: Boolean = false,
)
