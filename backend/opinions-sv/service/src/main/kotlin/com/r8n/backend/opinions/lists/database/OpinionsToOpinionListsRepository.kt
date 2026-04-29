package com.r8n.backend.opinions.lists.database

import com.r8n.backend.opinions.lists.persistence.OpinionsToOpinionListsPersistence
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface OpinionsToOpinionListsRepository : JpaRepository<OpinionsToOpinionListsPersistence, UUID> {
    fun findAllByOpinionList(opinionListId: UUID): List<OpinionsToOpinionListsPersistence>

    fun findAllByOpinion(opinionId: UUID): List<OpinionsToOpinionListsPersistence>

    @Query("SELECT otl.opinionList FROM OpinionsToOpinionListsPersistence otl WHERE otl.opinion IN :opinionIds")
    fun findOpinionListIdsByOpinionIn(
        @Param("opinionIds") opinionIds: Collection<UUID>,
    ): Set<UUID>

    fun countByOpinionList(opinionListId: UUID): Long
}
