package com.r8n.backend.opinions.facade

import com.r8n.backend.opinions.api.dto.OpinionDto
import com.r8n.backend.opinions.domain.Opinion
import com.r8n.backend.opinions.domain.toDto
import com.r8n.backend.opinions.service.OpinionService
import com.r8n.backend.users.integration.api.UsersInternalApi
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OpinionFacade(
    private val opinionService: OpinionService,
    private val usersInternalApi: UsersInternalApi,
) {
    fun getOpinion(opinionId: UUID): OpinionDto = opinionService.getOpinion(opinionId).toDto()

    fun getOpinionFor(subjectId: UUID): OpinionDto = opinionService.getOpinionFor(subjectId).toDto()

    private fun Opinion.toDto() =
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
            componentSection.components.map { it.toDto() }.toList(),
            status.toDto(),
            timestamp,
        )
}
