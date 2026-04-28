package com.r8n.backend.messaging.provider.database

import com.r8n.backend.messaging.persistence.ThreadPersistence
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ThreadRepository : JpaRepository<ThreadPersistence, UUID> {
    @Query(
        """
        SELECT thread FROM ThreadPersistence thread
        WHERE thread.requesterUserId = :userId
           OR thread.recipientUserId = :userId
        ORDER BY thread.updatedAt DESC
        """,
    )
    fun findVisibleByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Page<ThreadPersistence>

    @Query(
        """
        SELECT thread FROM ThreadPersistence thread
        WHERE thread.type = com.r8n.backend.messaging.domain.ThreadTypeEnum.DIRECT
          AND (
              (thread.requesterUserId = :firstUserId AND thread.recipientUserId = :secondUserId)
              OR
              (thread.requesterUserId = :secondUserId AND thread.recipientUserId = :firstUserId)
          )
        """,
    )
    fun findDirectThreadBetweenParticipants(
        firstUserId: UUID,
        secondUserId: UUID,
    ): ThreadPersistence?
}