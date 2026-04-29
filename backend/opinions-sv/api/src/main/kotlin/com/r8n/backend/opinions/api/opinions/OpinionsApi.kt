package com.r8n.backend.opinions.api.opinions

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.api.opinions.dto.ModerationDecisionDto
import com.r8n.backend.opinions.api.opinions.dto.OpinionDto
import com.r8n.backend.opinions.api.opinions.dto.RejectOpinionRequestDto
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

interface OpinionsApi {
    companion object {
        private const val ROOT_PATH = "/api/opinions"
        const val GET_BY_ID_PATH = "$ROOT_PATH/{id}"
        const val GET_FOR_SUBJECT_PATH = "$ROOT_PATH/for/{subjectId}"
        const val CREATE_PATH = ROOT_PATH
        const val UPDATE_PATH = "$ROOT_PATH/{opinionId}"
        const val DELETE_PATH = "$ROOT_PATH/{opinionId}"
        const val SUBMIT_FOR_MODERATION_PATH = "$ROOT_PATH/{opinionId}/submit-for-moderation"
        const val MODERATION_PATH = "$ROOT_PATH/moderation"
        const val MODERATION_DECISIONS_PATH = "$ROOT_PATH/moderation/decisions"
        const val APPROVE_PATH = "$ROOT_PATH/{opinionId}/approve"
        const val REJECT_PATH = "$ROOT_PATH/{opinionId}/reject"
        const val LINK_PATH = "$ROOT_PATH/link"
        const val UNLINK_PATH = "$ROOT_PATH/unlink/{linkId}"
        const val ADJUST_WEIGHT_PATH = "$ROOT_PATH/adjust-weight/{linkId}"
    }

    @GetMapping(GET_BY_ID_PATH)
    fun getOpinionById(
        @PathVariable id: UUID,
    ): OpinionDto

    @GetMapping(GET_FOR_SUBJECT_PATH)
    fun getOpinionFor(
        @PathVariable subjectId: UUID,
    ): OpinionDto

    @PostMapping(CREATE_PATH)
    fun createOpinion(
        @RequestParam(required = true)
        subjectId: UUID,
        @RequestParam(required = false)
        subjective: List<String>,
        @RequestParam(required = false)
        objective: List<String>,
        @RequestParam(required = false)
        mark: Double?,
    ): OpinionDto

    @PatchMapping(UPDATE_PATH)
    fun updateOpinion(
        @PathVariable opinionId: UUID,
        @RequestParam(required = false)
        subjective: List<String>,
        @RequestParam(required = false)
        objective: List<String>,
        @RequestParam(required = false)
        mark: Double?,
    ): OpinionDto

    @DeleteMapping(DELETE_PATH)
    fun deleteOpinion(
        @PathVariable opinionId: UUID,
    )

    @PostMapping(SUBMIT_FOR_MODERATION_PATH)
    fun submitOpinionForModeration(
        @PathVariable opinionId: UUID,
    ): OpinionDto

    @GetMapping(MODERATION_PATH)
    fun getModerationOpinions(
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionDto>

    @GetMapping(MODERATION_DECISIONS_PATH)
    fun getModerationDecisions(
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<ModerationDecisionDto>

    @PostMapping(APPROVE_PATH)
    fun approveOpinion(
        @PathVariable opinionId: UUID,
    ): OpinionDto

    @PostMapping(REJECT_PATH)
    fun rejectOpinion(
        @PathVariable opinionId: UUID,
        @Valid
        @RequestBody
        request: RejectOpinionRequestDto,
    ): OpinionDto

    @PostMapping(LINK_PATH)
    fun linkComponent(
        @RequestParam(required = true)
        parentOpinionId: UUID,
        @RequestParam(required = true)
        childOpinionId: UUID,
        @RequestParam(required = true)
        weight: Double,
    ): OpinionDto

    @DeleteMapping(UNLINK_PATH)
    fun unlinkComponent(
        @PathVariable linkId: UUID,
    ): OpinionDto

    @PatchMapping(ADJUST_WEIGHT_PATH)
    fun adjustComponentWeight(
        @PathVariable linkId: UUID,
        @RequestParam(required = true)
        weight: Double,
    ): OpinionDto
}
