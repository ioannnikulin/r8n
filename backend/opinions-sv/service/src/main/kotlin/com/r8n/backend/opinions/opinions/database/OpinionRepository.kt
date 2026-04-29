package com.r8n.backend.opinions.opinions.database

import com.r8n.backend.opinions.opinions.persistence.OpinionPersistence
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface OpinionRepository : JpaRepository<OpinionPersistence, UUID> {
    fun findFirstBySubjectAndOwnerOrderByTimestampDesc(
        subject: UUID,
        owner: UUID,
    ): OpinionPersistence?

    fun existsByIdAndOwner(
        id: UUID,
        owner: UUID,
    ): Boolean

    @Query("SELECT o.id FROM OpinionPersistence o WHERE o.subject IN :subjectIds")
    fun findIdsBySubjectIn(
        @Param("subjectIds") subjectIds: Collection<UUID>,
    ): Set<UUID>

    @Query("SELECT o.id FROM OpinionPersistence o WHERE o.timestamp >= :since")
    fun findIdsByTimestampAfter(
        @Param("since") since: Instant,
    ): Set<UUID>
}
