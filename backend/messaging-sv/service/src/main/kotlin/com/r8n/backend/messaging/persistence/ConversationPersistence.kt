package com.r8n.backend.messaging.persistence

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
@Table(schema = "messaging", name = "conversations")
class ConversationPersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    var type: ConversationTypeEnumPersistence,
    @Column(name = "created_by_user_id", nullable = false)
    var createdByUserId: UUID,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
    @Column(name = "last_message_at")
    var lastMessageAt: Instant? = null,
)

enum class ConversationTypeEnumPersistence {
    SUPPORT,
    DIRECT,
}
