package com.r8n.backend.opinions.provider.database

import com.r8n.backend.opinions.persistence.OpinionNotePersistence
import com.r8n.backend.opinions.persistence.OpinionNoteTypeEnum
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OpinionNoteRepository : JpaRepository<OpinionNotePersistence, UUID> {
    fun findAllByOpinionIdAndTypeOrderByIdAsc(
        opinionId: UUID,
        type: OpinionNoteTypeEnum,
    ): List<OpinionNotePersistence>

    fun deleteAllByOpinionId(opinionId: UUID)
}