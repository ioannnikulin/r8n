package com.r8n.backend.opinions.opinions.database

import com.r8n.backend.opinions.opinions.domain.OpinionStatusEnum
import com.r8n.backend.opinions.opinions.persistence.OpinionPersistence
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OpinionRepository : JpaRepository<OpinionPersistence, UUID> {
    fun findAllByStatus(
        status: OpinionStatusEnum,
        pageable: Pageable,
    ): Page<OpinionPersistence>

    fun findFirstBySubjectAndOwnerOrderByTimestampDesc(
        subject: UUID,
        owner: UUID,
    ): OpinionPersistence?

    fun existsByIdAndOwner(
        id: UUID,
        owner: UUID,
    ): Boolean
}
