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
@Table(schema = "users", name = "sessions")
class UserSessionPersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID,
//
    @Column(nullable = false)
    val userId: UUID,
//
    @Column(nullable = false)
    val created: Instant,
//
    @Column(nullable = false)
    val expires: Instant,
//
    @Column(nullable = false)
    val ip: String,
//
    @Column(nullable = false)
    val os: String,
//
    @Column(nullable = false)
    val userAgent: String,
)
