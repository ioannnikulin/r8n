package com.r8n.backend.opinions.opinions.service

import com.r8n.backend.opinions.opinions.database.OpinionNoteRepository
import com.r8n.backend.opinions.opinions.persistence.OpinionNotePersistence
import com.r8n.backend.opinions.opinions.persistence.OpinionNoteTypeEnum
import org.springframework.stereotype.Service
import java.util.UUID

// does not check permissions, so should be exposed only to OpinionService or another service that checks them
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

    fun replace(
        id: UUID,
        subjective: List<String>,
        objective: List<String>,
    ) {
        opinionNoteRepository.deleteAllByOpinionId(id)
        create(id, subjective, objective)
    }
}
