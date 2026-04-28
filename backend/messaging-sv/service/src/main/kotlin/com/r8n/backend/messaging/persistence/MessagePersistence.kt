package com.r8n.backend.messaging.persistence

import com.r8n.backend.messaging.domain.MessageAuthorRoleEnum
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
@Table(schema = "messaging", name = "messages")
class MessagePersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null,
//
    @Column(nullable = false)
    var threadId: UUID,
//
    @Column(nullable = false)
    var authorUserId: UUID,
//
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var authorRole: MessageAuthorRoleEnum,
//
    @Column(nullable = false)
    var text: String,
//
    @Column(nullable = false)
    var createdAt: Instant,
)