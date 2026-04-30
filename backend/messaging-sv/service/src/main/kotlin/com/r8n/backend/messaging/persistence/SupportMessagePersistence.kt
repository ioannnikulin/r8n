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
@Table(schema = "messaging", name = "support_messages")
class SupportMessagePersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null,
    @Column(name = "thread_id", nullable = false)
    var threadId: UUID,
    @Column(name = "author_user_id", nullable = false)
    var authorUserId: UUID,
    @Column(name = "author_role", nullable = false)
    @Enumerated(EnumType.STRING)
    var authorRole: SupportParticipantRoleEnumPersistence,
    @Column(name = "text", nullable = false)
    var text: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)

enum class SupportParticipantRoleEnumPersistence {
    USER,
    SUPPORT,
}
