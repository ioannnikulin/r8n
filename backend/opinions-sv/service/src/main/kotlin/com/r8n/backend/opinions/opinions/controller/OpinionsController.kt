package com.r8n.backend.opinions.opinions.controller

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.api.opinions.OpinionsApi
import com.r8n.backend.opinions.api.opinions.dto.OpinionDto
import com.r8n.backend.opinions.api.opinions.dto.RejectOpinionRequestDto
import com.r8n.backend.opinions.opinions.facade.OpinionFacade
import com.r8n.backend.security.Authority.IS_MODERATOR
import com.r8n.backend.security.Authority.IS_USER
import com.r8n.backend.security.Authority.IS_USER_OR_SERVICE
import com.r8n.backend.security.CurrentUserIdentifier.getCurrentUserId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class OpinionsController(
    private val opinionFacade: OpinionFacade,
) : OpinionsApi {
    @PreAuthorize(IS_USER_OR_SERVICE)
    override fun getOpinionById(id: UUID) = opinionFacade.getOpinion(id, getCurrentUserId())

    @PreAuthorize(IS_USER)
    override fun getOpinionFor(subjectId: UUID) = opinionFacade.getOpinionFor(subjectId, getCurrentUserId())

    @PreAuthorize(IS_USER)
    override fun createOpinion(
        subjectId: UUID,
        subjective: List<String>,
        objective: List<String>,
        mark: Double?,
    ): OpinionDto = opinionFacade.createOpinion(subjectId, subjective, objective, mark, getCurrentUserId())

    @PreAuthorize(IS_USER)
    override fun updateOpinion(
        opinionId: UUID,
        subjective: List<String>,
        objective: List<String>,
        mark: Double?,
    ) = opinionFacade.updateOpinion(opinionId, subjective, objective, mark, getCurrentUserId())

    @PreAuthorize(IS_USER)
    override fun deleteOpinion(opinionId: UUID) {
        opinionFacade.deleteOpinion(opinionId, getCurrentUserId())
    }

    @PreAuthorize(IS_USER)
    override fun submitOpinionForModeration(opinionId: UUID): OpinionDto =
        opinionFacade.submitOpinionForModeration(opinionId, getCurrentUserId())

    @PreAuthorize(IS_MODERATOR)
    override fun getModerationOpinions(pageable: PageRequestDto): PageResponseDto<OpinionDto> =
        opinionFacade.getModerationOpinions(pageable)

    @PreAuthorize(IS_MODERATOR)
    override fun getModerationDecisions(pageable: PageRequestDto) = opinionFacade.getModerationDecisions(pageable)

    @PreAuthorize(IS_MODERATOR)
    override fun approveOpinion(opinionId: UUID): OpinionDto =
        opinionFacade.approveOpinion(opinionId, getCurrentUserId())

    @PreAuthorize(IS_MODERATOR)
    override fun rejectOpinion(
        opinionId: UUID,
        request: RejectOpinionRequestDto,
    ): OpinionDto = opinionFacade.rejectOpinion(opinionId, getCurrentUserId(), request.reason)

    @PreAuthorize(IS_USER)
    override fun linkComponent(
        parentOpinionId: UUID,
        childOpinionId: UUID,
        weight: Double,
    ): OpinionDto = opinionFacade.linkComponent(parentOpinionId, childOpinionId, weight, getCurrentUserId())

    @PreAuthorize(IS_USER)
    override fun unlinkComponent(linkId: UUID): OpinionDto = opinionFacade.unlinkComponent(linkId, getCurrentUserId())

    @PreAuthorize(IS_USER)
    override fun adjustComponentWeight(
        linkId: UUID,
        weight: Double,
    ): OpinionDto = opinionFacade.adjustComponentWeight(linkId, weight, getCurrentUserId())
}
