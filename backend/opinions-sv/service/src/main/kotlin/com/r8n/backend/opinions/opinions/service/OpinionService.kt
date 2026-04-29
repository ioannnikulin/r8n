package com.r8n.backend.opinions.opinions.service

import com.r8n.backend.opinions.access.domain.OpinionPermissionEnum
import com.r8n.backend.opinions.access.service.AccessService
import com.r8n.backend.opinions.opinions.database.ModerationDecisionRepository
import com.r8n.backend.opinions.opinions.database.OpinionRepository
import com.r8n.backend.opinions.opinions.domain.ModerationDecision
import com.r8n.backend.opinions.opinions.domain.ModerationDecisionAction
import com.r8n.backend.opinions.opinions.domain.Opinion
import com.r8n.backend.opinions.opinions.domain.OpinionStatusEnum
import com.r8n.backend.opinions.opinions.persistence.ModerationDecisionPersistence
import com.r8n.backend.opinions.opinions.persistence.OpinionPersistence
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
    private val moderationDecisionRepository: ModerationDecisionRepository,
    private val accessService: AccessService,
) {
    fun getOpinion(
        id: UUID,
        requesterId: UUID,
    ): Opinion {
        val opinion = opinionRepository.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (!accessService.canAccessOpinion(requesterId, opinion.id!!, OpinionPermissionEnum.READ)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        return opinion.toDomain()
    }

    fun getOpinionFor(
        subjectId: UUID,
        requesterId: UUID,
    ): Opinion {
        val opinion =
            opinionRepository.findFirstBySubjectAndOwnerOrderByTimestampDesc(subjectId, requesterId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return opinion.toDomain()
    }

    @Transactional
    fun createOpinion(
        subjectId: UUID,
        subjective: List<String>,
        objective: List<String>,
        mark: Double?,
        creatorId: UUID,
    ): Opinion {
        val opinion =
            opinionRepository.save(
                OpinionPersistence(
                    owner = creatorId,
                    subject = subjectId,
                    mark = mark,
                    status = OpinionStatusEnum.DRAFT,
                    timestamp = Instant.now(),
                ),
            )
        noteService.create(opinion.id!!, subjective, objective)

        return opinion.toDomain()
    }

    @Transactional
    fun updateOpinion(
        opinionId: UUID,
        subjective: List<String>,
        objective: List<String>,
        mark: Double?,
        requesterId: UUID,
    ): Opinion {
        val opinion =
            opinionRepository
                .findById(
                    opinionId,
                ).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (!accessService.canAccessOpinion(requesterId, opinion.id!!, OpinionPermissionEnum.EDIT)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        opinion.mark = mark
        opinion.timestamp = Instant.now()
        val savedOpinion = opinionRepository.save(opinion)
        noteService.replace(savedOpinion.id!!, subjective, objective)
        return savedOpinion.toDomain()
    }

    @Transactional
    fun deleteOpinion(
        opinionId: UUID,
        requesterId: UUID,
    ) {
        val opinion =
            opinionRepository
                .findById(
                    opinionId,
                ).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (!accessService.canAccessOpinion(requesterId, opinion.id!!, OpinionPermissionEnum.DELETE)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        opinionRepository.delete(opinion)
    }

    @Transactional
    fun submitOpinionForModeration(
        opinionId: UUID,
        requesterId: UUID,
    ): Opinion {
        val opinion =
            opinionRepository
                .findById(
                    opinionId,
                ).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (opinion.owner != requesterId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only the opinion owner can submit it for moderation")
        }
        if (opinion.status != OpinionStatusEnum.DRAFT) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Only draft opinions can be submitted for moderation")
        }

        opinion.status = OpinionStatusEnum.PENDING_PREMODERATION
        opinion.timestamp = Instant.now()
        return opinionRepository.save(opinion).toDomain()
    }

    @Transactional(readOnly = true)
    fun getModerationOpinions(pageable: Pageable): Page<Opinion> =
        opinionRepository.findAllByStatus(OpinionStatusEnum.PENDING_PREMODERATION, pageable).map { it.toDomain() }

    @Transactional
    fun approveOpinion(
        opinionId: UUID,
        moderatorId: UUID,
    ): Opinion = transitionPendingOpinion(opinionId, OpinionStatusEnum.PUBLISHED, moderatorId, null)

    @Transactional
    fun rejectOpinion(
        opinionId: UUID,
        moderatorId: UUID,
        reason: String,
    ): Opinion {
        val trimmedReason = reason.trim()
        if (trimmedReason.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejection reason must not be blank")
        }

        return transitionPendingOpinion(opinionId, OpinionStatusEnum.REJECTED, moderatorId, trimmedReason)
    }

    @Transactional(readOnly = true)
    fun getModerationDecisions(pageable: Pageable): Page<ModerationDecision> =
        moderationDecisionRepository.findAllByOrderByCreatedAtDesc(pageable).map { it.toDomain() }

    @Transactional
    fun linkComponent(
        parentOpinionId: UUID,
        childOpinionId: UUID,
        weight: Double,
        requesterId: UUID,
    ): Opinion {
        if (parentOpinionId == childOpinionId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "An opinion cannot be linked to itself")
        }

        opinionRepository
            .findById(parentOpinionId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (!accessService.canAccessOpinion(requesterId, parentOpinionId, OpinionPermissionEnum.EDIT)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        opinionRepository
            .findById(childOpinionId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (!accessService.canAccessOpinion(requesterId, childOpinionId, OpinionPermissionEnum.READ)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        if (componentService.hasLink(parentOpinionId, childOpinionId)) {
            return getOpinion(parentOpinionId, requesterId)
        }
        try {
            componentService.linkComponent(parentOpinionId, childOpinionId, weight)
        } catch (_: DataIntegrityViolationException) {
            // Another concurrent request inserted the same link first: keep operation idempotent.
        }
        return getOpinion(parentOpinionId, requesterId)
    }

    @Transactional
    fun unlinkComponent(
        linkId: UUID,
        requesterId: UUID,
    ): Opinion {
        val parentOpinionId =
            componentService.getParentOpinionId(linkId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        opinionRepository
            .findById(parentOpinionId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (!accessService.canAccessOpinion(requesterId, parentOpinionId, OpinionPermissionEnum.EDIT)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        componentService.unlinkComponent(linkId)
        return getOpinion(parentOpinionId, requesterId)
    }

    @Transactional
    fun adjustComponentWeight(
        linkId: UUID,
        weight: Double,
        requesterId: UUID,
    ): Opinion {
        val parentOpinionId =
            componentService.getParentOpinionId(linkId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        opinionRepository
            .findById(parentOpinionId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (!accessService.canAccessOpinion(requesterId, parentOpinionId, OpinionPermissionEnum.EDIT)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        if (!componentService.adjustComponentWeight(linkId, weight)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return getOpinion(parentOpinionId, requesterId)
    }

    private fun transitionPendingOpinion(
        opinionId: UUID,
        targetStatus: OpinionStatusEnum,
        moderatorId: UUID,
        reason: String?,
    ): Opinion {
        val opinion =
            opinionRepository
                .findById(opinionId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (opinion.status != OpinionStatusEnum.PENDING_PREMODERATION) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only opinions pending premoderation can be moderated",
            )
        }

        val previousStatus = opinion.status
        opinion.status = targetStatus
        val decisionTimestamp = Instant.now()
        opinion.timestamp = decisionTimestamp
        val savedOpinion = opinionRepository.save(opinion)
        moderationDecisionRepository.save(
            ModerationDecisionPersistence(
                opinion = savedOpinion.id!!,
                moderator = moderatorId,
                action = targetStatus.toModerationDecisionAction(),
                previousStatus = previousStatus,
                newStatus = targetStatus,
                reason = reason,
                createdAt = decisionTimestamp,
            ),
        )
        return savedOpinion.toDomain()
    }

    private fun OpinionStatusEnum.toModerationDecisionAction(): ModerationDecisionAction =
        when (this) {
            OpinionStatusEnum.PUBLISHED -> ModerationDecisionAction.APPROVED
            OpinionStatusEnum.REJECTED -> ModerationDecisionAction.REJECTED
            OpinionStatusEnum.DRAFT,
            OpinionStatusEnum.PENDING_PREMODERATION,
                -> throw IllegalArgumentException("Unsupported moderation decision status: $this")
        }

    private fun OpinionPersistence.toDomain(): Opinion =
        Opinion(
            id!!,
            owner,
            subject,
            subjectService.getSubjectName(subject) ?: "UNNAMED",
            noteService.getSubjective(id!!),
            noteService.getObjective(id!!),
            mark,
            componentService.getComponentSection(id!!),
            status,
            timestamp,
        )

    private fun ModerationDecisionPersistence.toDomain(): ModerationDecision {
        val opinionPersistence =
            opinionRepository
                .findById(opinion)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        val opinionId = opinionPersistence.id ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

        return ModerationDecision(
            id!!,
            opinionId,
            subjectService.getSubjectName(opinionPersistence.subject) ?: "UNNAMED",
            opinionPersistence.owner,
            moderator,
            action,
            previousStatus,
            newStatus,
            reason,
            createdAt,
        )
    }
}
