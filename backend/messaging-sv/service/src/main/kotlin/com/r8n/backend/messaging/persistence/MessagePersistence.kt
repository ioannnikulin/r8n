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
@Table(schema = "messaging", name = "messages")
class MessagePersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null,
    @Column(name = "conversation_id", nullable = false)
    var conversationId: UUID,
    @Column(name = "author_user_id", nullable = false)
    var authorUserId: UUID,
    @Column(name = "author_display_name_snapshot", nullable = false)
    var authorDisplayNameSnapshot: String,
    @Column(name = "author_role_snapshot", nullable = false)
    @Enumerated(EnumType.STRING)
    var authorRoleSnapshot: MessageAuthorRoleEnumPersistence,
    @Column(name = "text", nullable = false)
    var text: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)

enum class MessageAuthorRoleEnumPersistence {
    USER,
    MODERATOR,
    SUPPORT,
    ADMIN,
}
