package com.r8n.backend.messaging.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    schema = "messaging",
    name = "conversation_participants",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_conversation_participants_conversation_user",
            columnNames = ["conversation_id", "user_id"],
        ),
    ],
)
class ConversationParticipantPersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null,
    @Column(name = "conversation_id", nullable = false)
    var conversationId: UUID,
    @Column(name = "user_id", nullable = false)
    var userId: UUID,
    @Column(name = "participant_role", nullable = false)
    @Enumerated(EnumType.STRING)
    var participantRole: ConversationParticipantRoleEnumPersistence,
    @Column(name = "joined_at", nullable = false)
    var joinedAt: Instant,
    @Column(name = "archived_at")
    var archivedAt: Instant? = null,
    @Column(name = "last_read_at")
    var lastReadAt: Instant? = null,
)

enum class ConversationParticipantRoleEnumPersistence {
    MEMBER,
    SUPPORT_AGENT,
}
