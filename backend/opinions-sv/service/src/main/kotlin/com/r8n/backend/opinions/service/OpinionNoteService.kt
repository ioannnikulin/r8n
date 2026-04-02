package com.r8n.backend.opinions.service

import com.r8n.backend.opinions.persistence.OpinionNotePersistence
import com.r8n.backend.opinions.persistence.OpinionNoteTypeEnum
import com.r8n.backend.opinions.provider.database.OpinionNoteRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OpinionNoteService(
    private val opinionNoteRepository: OpinionNoteRepository,
) {
    fun getSubjective(id: UUID) =
        opinionNoteRepository
            .findAllByOpinionIdAndTypeOrderByIdAsc(id, OpinionNoteTypeEnum.SUBJECTIVE)
            .map { it.description }

    fun getObjective(id: UUID) =
        opinionNoteRepository
            .findAllByOpinionIdAndTypeOrderByIdAsc(id, OpinionNoteTypeEnum.OBJECTIVE)
            .map { it.description }

    fun create(
        id: UUID,
        subjective: List<String>,
        objective: List<String>,
    ) {
        val notes =
            buildList {
                subjective.forEach {
                    add(OpinionNotePersistence(opinionId = id, type = OpinionNoteTypeEnum.SUBJECTIVE, description = it))
                }
                objective.forEach {
                    add(OpinionNotePersistence(opinionId = id, type = OpinionNoteTypeEnum.OBJECTIVE, description = it))
                }
            }
        opinionNoteRepository.saveAll(notes)
    }
}