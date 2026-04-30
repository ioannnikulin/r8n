package com.r8n.backend.messaging.provider.database

import com.r8n.backend.messaging.persistence.SupportThreadPersistence
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface SupportThreadRepository : JpaRepository<SupportThreadPersistence, UUID> {
    @Query(
        """
        SELECT thread
        FROM SupportThreadPersistence thread
        WHERE (:ownerUserId IS NULL OR thread.ownerUserId = :ownerUserId)
        ORDER BY (
            SELECT MAX(message.createdAt)
            FROM SupportMessagePersistence message
            WHERE message.threadId = thread.id
        ) DESC
        """,
        countQuery = """
            SELECT COUNT(thread)
            FROM SupportThreadPersistence thread
            WHERE (:ownerUserId IS NULL OR thread.ownerUserId = :ownerUserId)
        """,
    )
    fun findVisibleOrderByLastMessageAtDesc(
        @Param("ownerUserId")
        ownerUserId: UUID?,
        pageable: Pageable,
    ): Page<SupportThreadPersistence>

    @Query(
        """
        SELECT
            thread.id AS id,
            thread.ownerUserId AS ownerUserId,
            MIN(message.createdAt) AS createdAt,
            MAX(message.createdAt) AS lastMessageAt
        FROM SupportThreadPersistence thread
        JOIN SupportMessagePersistence message ON message.threadId = thread.id
        WHERE (:ownerUserId IS NULL OR thread.ownerUserId = :ownerUserId)
        GROUP BY thread.id, thread.ownerUserId
        ORDER BY MAX(message.createdAt) DESC
        """,
        countQuery = """
            SELECT COUNT(DISTINCT thread.id)
            FROM SupportThreadPersistence thread
            JOIN SupportMessagePersistence message ON message.threadId = thread.id
            WHERE (:ownerUserId IS NULL OR thread.ownerUserId = :ownerUserId)
        """,
    )
    fun findVisibleSummariesOrderByLastMessageAtDesc(
        @Param("ownerUserId")
        ownerUserId: UUID?,
        pageable: Pageable,
    ): Page<SupportThreadSummaryProjection>
}

interface SupportThreadSummaryProjection {
    val id: UUID
    val ownerUserId: UUID
    val createdAt: Instant
    val lastMessageAt: Instant
}
