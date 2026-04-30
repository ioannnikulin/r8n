package com.r8n.backend.opinions.lists.database

import com.r8n.backend.opinions.lists.persistence.OpinionListSyncPersistence
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface OpinionListSyncRepository : CrudRepository<OpinionListSyncPersistence, UUID> {
    fun deleteByDestinationListAndSourceList(
        destinationList: UUID,
        sourceList: UUID,
    )

    fun findByDestinationListAndSourceList(
        destinationList: UUID,
        sourceList: UUID,
    ): OpinionListSyncPersistence?

    fun findAllByDestinationList(destinationList: UUID): List<OpinionListSyncPersistence>
}
