package com.r8n.backend.users.provider.database

import com.r8n.backend.users.persistence.UserSessionPersistence
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserSessionRepository : JpaRepository<UserSessionPersistence, UUID> {
    fun findAllByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Page<UserSessionPersistence>

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): UserSessionPersistence?

    fun findFirstByUserIdOrderByCreatedDesc(userId: UUID): UserSessionPersistence?
}
