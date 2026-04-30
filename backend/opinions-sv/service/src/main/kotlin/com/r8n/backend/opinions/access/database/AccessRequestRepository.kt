package com.r8n.backend.opinions.access.database

import com.r8n.backend.opinions.access.domain.RequestStatusEnum
import com.r8n.backend.opinions.access.persistence.AccessRequestPersistence
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface AccessRequestRepository : JpaRepository<AccessRequestPersistence, UUID> {
    fun findByRequesterAndListAndStatusIn(
        requesterId: UUID,
        listId: UUID,
        statuses: List<RequestStatusEnum>,
    ): List<AccessRequestPersistence>

    @Query(
        """
        SELECT ar FROM AccessRequestPersistence ar
        JOIN OpinionListPersistence ol ON ar.list = ol.id
        WHERE (:listId IS NULL OR ar.list = :listId)
        AND (:requesterId IS NULL OR ar.requester = :requesterId)
        AND (:status IS NULL OR ar.status = :status)
        AND (:ownerId IS NULL OR ol.owner = :ownerId)
    """,
    )
    fun findAllByFilters(
        listId: UUID?,
        requesterId: UUID?,
        ownerId: UUID?,
        status: RequestStatusEnum?,
        pageable: Pageable,
    ): Page<AccessRequestPersistence>

    @Query(
        """
        SELECT ar FROM AccessRequestPersistence ar
        JOIN OpinionListPersistence ol ON ar.list = ol.id
        WHERE (:listId IS NULL OR ar.list = :listId)
        AND (:requesterId IS NULL OR ar.requester = :requesterId)
        AND (:status IS NULL OR ar.status = :status)
        AND (:ownerId IS NULL OR ol.owner = :ownerId)
        AND ar.updatedAt > :since
    """,
    )
    fun findAllByFiltersUpdatedSince(
        listId: UUID?,
        requesterId: UUID?,
        ownerId: UUID?,
        since: Instant,
        status: RequestStatusEnum?,
        pageable: Pageable,
    ): Page<AccessRequestPersistence>

    fun existsByRequesterAndListAndStatus(
        requester: UUID,
        list: UUID,
        status: RequestStatusEnum,
    ): Boolean

    fun countByListAndStatus(
        list: UUID,
        status: RequestStatusEnum,
    ): Long
}
