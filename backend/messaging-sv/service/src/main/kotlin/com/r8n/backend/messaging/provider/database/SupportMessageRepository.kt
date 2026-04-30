package com.r8n.backend.messaging.provider.database

import com.r8n.backend.messaging.persistence.SupportMessagePersistence
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SupportMessageRepository : JpaRepository<SupportMessagePersistence, UUID> {
    fun findAllByThreadIdOrderByCreatedAtAsc(
        threadId: UUID,
        pageable: Pageable,
    ): Page<SupportMessagePersistence>

    fun findAllByThreadIdInOrderByThreadIdAscCreatedAtAsc(threadIds: Collection<UUID>): List<SupportMessagePersistence>
}
