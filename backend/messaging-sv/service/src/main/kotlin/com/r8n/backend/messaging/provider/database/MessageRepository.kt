package com.r8n.backend.messaging.provider.database

import com.r8n.backend.messaging.persistence.MessagePersistence
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MessageRepository : JpaRepository<MessagePersistence, UUID> {
    fun findByThreadIdOrderByCreatedAtAsc(
        threadId: UUID,
        pageable: Pageable,
    ): Page<MessagePersistence>

    fun findFirstByThreadIdOrderByCreatedAtDesc(threadId: UUID): MessagePersistence?

    fun countByThreadIdAndCreatedAtAfter(
        threadId: UUID,
        createdAt: java.time.Instant,
    ): Long

    fun countByThreadId(threadId: UUID): Long
}