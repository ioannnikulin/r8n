package com.r8n.backend.users.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(schema = "users", name = "pii")
class PIIPersistence(
    @Id
    val userId: UUID,
//
    @Column(nullable = false)
    val name: String,
//
    @Column(nullable = false)
    val email: String,
//
    @Column(nullable = true)
    val phone: String?,
//
    @Column(nullable = true)
    val about: String?,
//
    @Column(nullable = true)
    val location: String?,
)
