package com.r8n.backend.opinions.lists.database

import com.r8n.backend.opinions.lists.domain.OpinionListPrivacyEnum
import com.r8n.backend.opinions.lists.persistence.OpinionListPersistence
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface OpinionListRepository : JpaRepository<OpinionListPersistence, UUID> {
    fun existsByIdAndOwner(
        id: UUID,
        owner: UUID,
    ): Boolean

    fun findByOwner(
        owner: UUID,
        pageable: Pageable,
    ): Page<OpinionListPersistence>

    @Query(
        """
        SELECT ol FROM OpinionListPersistence ol
        WHERE LOWER(ol.name) LIKE LOWER(CONCAT('%', :nameSubstring, '%'))
        AND (ol.owner = :requesterId OR ol.privacy = :searchablePrivacy)
        """,
    )
    fun findByNameContainingIgnoreCaseAndPrivacyFilter(
        @Param("nameSubstring") nameSubstring: String,
        @Param("requesterId") requesterId: UUID,
        @Param("searchablePrivacy") searchablePrivacy: OpinionListPrivacyEnum,
        pageable: Pageable,
    ): Page<OpinionListPersistence>
}
