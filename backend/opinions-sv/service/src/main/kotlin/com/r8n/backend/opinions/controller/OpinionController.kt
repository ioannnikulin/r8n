package com.r8n.backend.opinions.controller

import com.r8n.backend.mock.stub.OpinionTestDataFactory
import com.r8n.backend.opinions.api.OpinionApi
import com.r8n.backend.opinions.api.dto.OpinionDto
import com.r8n.backend.opinions.facade.OpinionFacade
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class OpinionController(
    private val opinionFacade: OpinionFacade,
) : OpinionApi {
    override fun getOpinionById(id: UUID) = opinionFacade.getOpinion(id)

    override fun getOpinionFor(subjectId: UUID) = opinionFacade.getOpinionFor(subjectId)

    override fun createOpinion(
        subjectId: UUID,
        subjective: List<String>,
        objective: List<String>,
        mark: Double?,
    ) = OpinionTestDataFactory.postOpinion(
        subjectId = subjectId,
        subjective = subjective,
        objective = objective,
        mark = mark,
    )

    override fun updateOpinion(
        opinionId: UUID,
        subjective: List<String>,
        objective: List<String>,
        mark: Double?,
    ) = OpinionTestDataFactory.postOpinion(
        opinionId = opinionId,
        subjective = subjective,
        objective = objective,
        mark = mark,
    )

    @PreAuthorize("hasRole('ADMIN')")
    override fun deleteOpinion(
        opinionId: UUID,
    ) {
    }

    override fun linkComponent(
        parentOpinionId: UUID,
        childOpinionId: UUID,
        weight: Double,
    ) = OpinionTestDataFactory.getOpinion(parentOpinionId)

    override fun unlinkComponent(linkId: UUID): OpinionDto = OpinionTestDataFactory.getOpinion(UUID.fromString("0"))

    override fun adjustComponentWeight(
        linkId: UUID,
        weight: Double,
    ): OpinionDto =
        OpinionTestDataFactory.getOpinion(UUID.fromString("0"))

}
