package com.r8n.backend.messaging.provider.database

import com.r8n.backend.messaging.persistence.ConversationParticipantPersistence
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ConversationParticipantRepository : JpaRepository<ConversationParticipantPersistence, UUID> {
    fun findAllByUserId(userId: UUID): List<ConversationParticipantPersistence>

    fun findAllByConversationIdIn(conversationIds: Collection<UUID>): List<ConversationParticipantPersistence>

    fun findAllByConversationId(conversationId: UUID): List<ConversationParticipantPersistence>

    fun existsByConversationIdAndUserId(
        conversationId: UUID,
        userId: UUID,
    ): Boolean

    fun findByConversationIdAndUserId(
        conversationId: UUID,
        userId: UUID,
    ): ConversationParticipantPersistence?
}
