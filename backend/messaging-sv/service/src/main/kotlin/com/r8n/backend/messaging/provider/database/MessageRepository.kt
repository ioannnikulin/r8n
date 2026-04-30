package com.r8n.backend.messaging.provider.database

import com.r8n.backend.messaging.persistence.MessagePersistence
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface MessageRepository : JpaRepository<MessagePersistence, UUID> {
    fun findAllByConversationIdOrderByCreatedAtAsc(
        conversationId: UUID,
        pageable: Pageable,
    ): Page<MessagePersistence>

    fun findAllByConversationIdInOrderByConversationIdAscCreatedAtAsc(
        conversationIds: Collection<UUID>,
    ): List<MessagePersistence>

    @Query(
        """
        SELECT COUNT(message)
        FROM MessagePersistence message
        JOIN ConversationParticipantPersistence participant
          ON participant.conversationId = message.conversationId
        JOIN ConversationPersistence conversation
          ON conversation.id = message.conversationId
        WHERE conversation.type = com.r8n.backend.messaging.persistence.ConversationTypeEnumPersistence.DIRECT
          AND participant.userId = :userId
          AND message.authorUserId <> :userId
          AND (participant.lastReadAt IS NULL OR message.createdAt > participant.lastReadAt)
        """,
    )
    fun countUnreadMessagesForParticipant(
        @Param("userId")
        userId: UUID,
    ): Long
}
