package com.r8n.backend.opinions.opinions.facade

import com.r8n.backend.opinions.api.opinions.dto.ModerationDecisionActionDto
import com.r8n.backend.opinions.api.opinions.dto.ModerationDecisionDto
import com.r8n.backend.opinions.api.opinions.dto.OpinionDto
import com.r8n.backend.opinions.api.opinions.dto.OpinionRowDto
import com.r8n.backend.opinions.api.opinions.dto.OpinionStatusEnumDto
import com.r8n.backend.opinions.api.opinions.dto.WeightedOpinionReferenceDto
import com.r8n.backend.opinions.opinions.domain.ModerationDecision
import com.r8n.backend.opinions.opinions.domain.ModerationDecisionAction
import com.r8n.backend.opinions.opinions.domain.Opinion
import com.r8n.backend.opinions.opinions.domain.OpinionStatusEnum
import com.r8n.backend.opinions.opinions.domain.WeightedOpinionReference
import com.r8n.backend.users.integration.api.UsersInternalApi
import org.springframework.stereotype.Component

@Component
class OpinionMapper(
    private val usersInternalApi: UsersInternalApi,
) {
    fun toDto(opinion: Opinion): OpinionDto =
        with(opinion) {
            OpinionDto(
                id,
                owner,
                usersInternalApi.getUserName(owner),
                subject,
                subjectName,
                subjective,
                objective,
                mark,
                componentSection.componentMark,
                componentSection.components.map { toDto(it) }.toList(),
                status.toDto(),
                timestamp,
            )
        }

    fun toDto(decision: ModerationDecision): ModerationDecisionDto =
        with(decision) {
            ModerationDecisionDto(
                id = id,
                opinionId = opinionId,
                subjectName = subjectName,
                ownerName = usersInternalApi.getUserName(owner),
                moderatorId = moderator,
                moderatorName = usersInternalApi.getUserName(moderator),
                action = action.toDto(),
                previousStatus = previousStatus.toDto(),
                newStatus = newStatus.toDto(),
                reason = reason,
                createdAt = createdAt,
            )
        }

    private fun ModerationDecisionAction.toDto(): ModerationDecisionActionDto =
        when (this) {
            ModerationDecisionAction.APPROVED -> ModerationDecisionActionDto.APPROVED
            ModerationDecisionAction.REJECTED -> ModerationDecisionActionDto.REJECTED
        }

    fun OpinionStatusEnum.toDto(): OpinionStatusEnumDto =
        when (this) {
            OpinionStatusEnum.DRAFT -> OpinionStatusEnumDto.DRAFT
            OpinionStatusEnum.PENDING_PREMODERATION -> OpinionStatusEnumDto.PENDING_PREMODERATION
            OpinionStatusEnum.PUBLISHED -> OpinionStatusEnumDto.PUBLISHED
            OpinionStatusEnum.REJECTED -> OpinionStatusEnumDto.REJECTED
        }

    fun OpinionStatusEnumDto.fromDto(): OpinionStatusEnum =
        when (this) {
            OpinionStatusEnumDto.DRAFT -> OpinionStatusEnum.DRAFT
            OpinionStatusEnumDto.PENDING_PREMODERATION -> OpinionStatusEnum.PENDING_PREMODERATION
            OpinionStatusEnumDto.PUBLISHED -> OpinionStatusEnum.PUBLISHED
            OpinionStatusEnumDto.REJECTED -> OpinionStatusEnum.REJECTED
        }

    fun toDto(opRef: WeightedOpinionReference) =
        with(opRef) {
            WeightedOpinionReferenceDto(
                id,
                opinion,
                weight,
            )
        }

    fun toRowDto(opinion: Opinion): OpinionRowDto =
        OpinionRowDto(
            opinionId = opinion.id,
            owner = opinion.owner,
            ownerName = usersInternalApi.getUserName(opinion.owner),
            subjective = opinion.subjective,
            objective = opinion.objective,
            mark = opinion.mark,
            status = opinion.status.toDto(),
            timestamp = opinion.timestamp,
            weight = opinion.weight ?: 1.0,
        )

    fun fromDto(opRef: WeightedOpinionReferenceDto) =
        with(opRef) {
            WeightedOpinionReference(
                id,
                opinion,
                weight,
            )
        }
}
