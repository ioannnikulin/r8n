package com.r8n.backend.opinions.opinions.database

import com.r8n.backend.opinions.opinions.persistence.OpinionSubjectPersistence
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface OpinionSubjectRepository : JpaRepository<OpinionSubjectPersistence, UUID> {
    fun findByNameContainingIgnoreCaseOrderByNameAsc(
        name: String,
        pageable: Pageable,
    ): Page<OpinionSubjectPersistence>

    @Query(
        """
        SELECT s FROM OpinionSubjectPersistence s
        WHERE (:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:referentId IS NULL OR s.referent = :referentId)
        ORDER BY s.name ASC
        """,
    )
    fun findByOptionalFilters(
        name: String?,
        referentId: UUID?,
        pageable: Pageable,
    ): Page<OpinionSubjectPersistence>

    @Query("SELECT s.id FROM OpinionSubjectPersistence s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun findIdsByNameContainingIgnoreCase(
        @Param("name") name: String,
    ): Set<UUID>

    @Query("SELECT s.id FROM OpinionSubjectPersistence s WHERE s.referent IN :referentIds")
    fun findIdsByReferentIn(
        @Param("referentIds") referentIds: Collection<UUID>,
    ): Set<UUID>
}
