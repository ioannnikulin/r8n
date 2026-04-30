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
@Table(schema = "users", name = "api_keys")
class ApiKeyPersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null,
//
    @Column(nullable = false)
    val userId: UUID,
//
    @Column(nullable = false, unique = true)
    val keyIdentifier: String,
//
    @Column(nullable = false)
    val keyHash: String,
//
    @Column(nullable = false)
    val name: String,
//
    @Column(nullable = false)
    val createdAt: Instant,
//
    @Column(nullable = true)
    var lastUsedAt: Instant? = null,
//
    @Column(nullable = true)
    val expiresAt: Instant? = null,
//
    @Column(nullable = false)
    var revoked: Boolean = false,
)
