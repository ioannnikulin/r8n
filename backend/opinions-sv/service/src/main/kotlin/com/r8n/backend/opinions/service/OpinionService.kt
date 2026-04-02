package com.r8n.backend.opinions.service

import com.r8n.backend.opinions.domain.Opinion
import com.r8n.backend.opinions.domain.OpinionStatusEnum
import com.r8n.backend.opinions.persistence.OpinionPersistence
import com.r8n.backend.opinions.provider.database.OpinionRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class OpinionService(
    private val subjectService: SubjectService,
    private val noteService: OpinionNoteService,
    private val componentService: ComponentService,
    private val opinionRepository: OpinionRepository,
) {
    fun getOpinion(id: UUID): Opinion {
        val opinion = opinionRepository.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        return toDomain(opinion)
    }

    fun getOpinionFor(subjectId: UUID): Opinion {
        val opinion =
            opinionRepository.findFirstBySubjectOrderByTimestampDesc(subjectId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return toDomain(opinion)
    }

    @Transactional
    fun createOpinion(
        subjectId: UUID,
        subjective: List<String>,
        objective: List<String>,
        mark: Double?,
    ): Opinion {
        val opinion =
            opinionRepository.save(
                OpinionPersistence(
                    owner = STUB_OWNER_ID,
                    subject = subjectId,
                    mark = mark,
                    status = OpinionStatusEnum.DRAFT,
                    timestamp = Instant.now(),
                ),
            )
        noteService.create(opinion.id!!, subjective, objective)
        return toDomain(opinion)
    }

    private fun toDomain(opinion: OpinionPersistence): Opinion =
        Opinion(
            opinion.id!!,
            opinion.owner,
            opinion.subject,
            subjectService.getSubjectName(opinion.subject),
            noteService.getSubjective(opinion.id!!),
            noteService.getObjective(opinion.id!!),
            opinion.mark,
            componentService.getComponentSection(opinion.id!!),
            opinion.status,
            opinion.timestamp,
        )

    private companion object {
        val STUB_OWNER_ID: UUID = UUID.fromString("10101010-1010-1010-1010-101010101010")
    }
}