package com.r8n.backend.opinions.opinions.database

import com.r8n.backend.opinions.opinions.persistence.ReferentPersistence
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ReferentRepository : JpaRepository<ReferentPersistence, UUID> {
    fun findByNameContainingIgnoreCaseOrderByNameAsc(
        name: String,
        pageable: Pageable,
    ): Page<ReferentPersistence>

    fun findAllByOrderByNameAsc(pageable: Pageable): Page<ReferentPersistence>

    fun findAllByReferentGroupOrderByIdAsc(referentGroup: UUID): List<ReferentPersistence>

    @Query("SELECT r.id FROM ReferentPersistence r WHERE LOWER(r.address) LIKE LOWER(CONCAT('%', :address, '%'))")
    fun findIdsByAddressContainingIgnoreCase(
        @Param("address") address: String,
    ): Set<UUID>

    @Query(
        """
        SELECT r.id FROM ReferentPersistence r
        WHERE r.latitude IS NOT NULL AND r.longitude IS NOT NULL
        AND (6371000 * acos(
            least(1.0, greatest(-1.0, cos(radians(:lat)) * cos(radians(r.latitude)) * cos(radians(r.longitude) - radians(:lng)) +
            sin(radians(:lat)) * sin(radians(r.latitude))))
        )) <= :radius
        """,
    )
    fun findIdsByLocationRadius(
        @Param("lat") lat: Double,
        @Param("lng") lng: Double,
        @Param("radius") radiusInMeters: Double,
    ): Set<UUID>
}
