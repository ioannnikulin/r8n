package com.r8n.backend.messaging.provider.database

import com.r8n.backend.messaging.persistence.ConversationPersistence
import com.r8n.backend.messaging.persistence.ConversationTypeEnumPersistence
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ConversationRepository : JpaRepository<ConversationPersistence, UUID> {
    fun findAllByTypeOrderByLastMessageAtDesc(
        type: ConversationTypeEnumPersistence,
        pageable: Pageable,
    ): Page<ConversationPersistence>

    @Query(
        """
        SELECT conversation
        FROM ConversationPersistence conversation
        JOIN ConversationParticipantPersistence participant
            ON participant.conversationId = conversation.id
        WHERE conversation.type = :type
          AND participant.userId = :userId
        ORDER BY conversation.lastMessageAt DESC NULLS LAST, conversation.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(conversation)
        FROM ConversationPersistence conversation
        JOIN ConversationParticipantPersistence participant
            ON participant.conversationId = conversation.id
        WHERE conversation.type = :type
          AND participant.userId = :userId
        """,
    )
    fun findVisibleByTypeAndParticipantOrderByLastMessageAtDesc(
        @Param("type")
        type: ConversationTypeEnumPersistence,
        @Param("userId")
        userId: UUID,
        pageable: Pageable,
    ): Page<ConversationPersistence>

    @Query(value = "SELECT pg_advisory_xact_lock(hashtextextended(:pairKey, 0))", nativeQuery = true)
    fun lockDirectConversationPair(
        @Param("pairKey")
        pairKey: String,
    )

    @Query(
        """
        SELECT conversation
        FROM ConversationPersistence conversation
        WHERE conversation.type = :type
          AND EXISTS (
              SELECT participantA
              FROM ConversationParticipantPersistence participantA
              WHERE participantA.conversationId = conversation.id
                AND participantA.userId = :firstUserId
          )
          AND EXISTS (
              SELECT participantB
              FROM ConversationParticipantPersistence participantB
              WHERE participantB.conversationId = conversation.id
                AND participantB.userId = :secondUserId
          )
          AND (
              SELECT COUNT(participant)
              FROM ConversationParticipantPersistence participant
              WHERE participant.conversationId = conversation.id
          ) = 2
        """,
    )
    fun findDirectConversationBetweenUsers(
        @Param("type")
        type: ConversationTypeEnumPersistence,
        @Param("firstUserId")
        firstUserId: UUID,
        @Param("secondUserId")
        secondUserId: UUID,
    ): ConversationPersistence?
}
