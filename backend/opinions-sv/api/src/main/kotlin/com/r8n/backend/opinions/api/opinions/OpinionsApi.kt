package com.r8n.backend.opinions.api.opinions

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.api.opinions.dto.ModerationDecisionDto
import com.r8n.backend.opinions.api.opinions.dto.OpinionDto
import com.r8n.backend.opinions.api.opinions.dto.RejectOpinionRequestDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Tag(name = "Opinions", description = "Opinion authoring, component linking, and moderation endpoints.")
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
    @Operation(
        summary = "Get opinion by id",
        description = "Returns an opinion visible to the authenticated actor.",
    )
    fun getOpinionById(
        @Parameter(description = "Opinion identifier.")
        @PathVariable id: UUID,
    ): OpinionDto

    @GetMapping(GET_FOR_SUBJECT_PATH)
    @Operation(
        summary = "Get my opinion for subject",
        description = "Returns the authenticated user's opinion for a subject.",
    )
    fun getOpinionFor(
        @Parameter(description = "Subject identifier.")
        @PathVariable subjectId: UUID,
    ): OpinionDto

    @PostMapping(CREATE_PATH)
    @Operation(
        summary = "Create opinion",
        description = "Creates an opinion for a subject with subjective text, objective text, and optional mark.",
    )
    fun createOpinion(
        @Parameter(description = "Subject identifier.")
        @RequestParam(required = true)
        subjectId: UUID,
        @Parameter(description = "Subjective opinion statements.")
        @RequestParam(required = false)
        subjective: List<String>,
        @Parameter(description = "Objective supporting statements.")
        @RequestParam(required = false)
        objective: List<String>,
        @Parameter(description = "Optional numeric mark.")
        @RequestParam(required = false)
        mark: Double?,
    ): OpinionDto

    @PatchMapping(UPDATE_PATH)
    @Operation(
        summary = "Update opinion",
        description = "Updates editable fields on an opinion owned by the authenticated user.",
    )
    fun updateOpinion(
        @Parameter(description = "Opinion identifier.")
        @PathVariable opinionId: UUID,
        @Parameter(description = "Replacement subjective opinion statements.")
        @RequestParam(required = false)
        subjective: List<String>,
        @Parameter(description = "Replacement objective supporting statements.")
        @RequestParam(required = false)
        objective: List<String>,
        @Parameter(description = "Replacement numeric mark.")
        @RequestParam(required = false)
        mark: Double?,
    ): OpinionDto

    @DeleteMapping(DELETE_PATH)
    @Operation(
        summary = "Delete opinion",
        description = "Deletes an opinion owned by the authenticated user.",
    )
    fun deleteOpinion(
        @Parameter(description = "Opinion identifier.")
        @PathVariable opinionId: UUID,
    )

    @PostMapping(SUBMIT_FOR_MODERATION_PATH)
    @Operation(
        summary = "Submit opinion for moderation",
        description = "Moves an opinion into the moderation workflow before publication.",
    )
    fun submitOpinionForModeration(
        @Parameter(description = "Opinion identifier.")
        @PathVariable opinionId: UUID,
    ): OpinionDto

    @GetMapping(MODERATION_PATH)
    @Operation(
        summary = "List opinions for moderation",
        description = "Returns opinions waiting for moderator review.",
    )
    fun getModerationOpinions(
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionDto>

    @GetMapping(MODERATION_DECISIONS_PATH)
    @Operation(
        summary = "List moderation decisions",
        description = "Returns historical moderation decisions for audit and review.",
    )
    fun getModerationDecisions(
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<ModerationDecisionDto>

    @PostMapping(APPROVE_PATH)
    @Operation(
        summary = "Approve opinion",
        description = "Approves an opinion in moderation and makes the decision auditable.",
    )
    fun approveOpinion(
        @Parameter(description = "Opinion identifier.")
        @PathVariable opinionId: UUID,
    ): OpinionDto

    @PostMapping(REJECT_PATH)
    @Operation(
        summary = "Reject opinion",
        description = "Rejects an opinion in moderation with a reason visible through moderation decision history.",
    )
    fun rejectOpinion(
        @Parameter(description = "Opinion identifier.")
        @PathVariable opinionId: UUID,
        @Valid
        @RequestBody
        request: RejectOpinionRequestDto,
    ): OpinionDto

    @PostMapping(LINK_PATH)
    @Operation(
        summary = "Link component opinion",
        description = "Links a child opinion as a weighted component of a parent opinion.",
    )
    fun linkComponent(
        @Parameter(description = "Parent opinion identifier.")
        @RequestParam(required = true)
        parentOpinionId: UUID,
        @Parameter(description = "Child opinion identifier.")
        @RequestParam(required = true)
        childOpinionId: UUID,
        @Parameter(description = "Component weight.")
        @RequestParam(required = true)
        weight: Double,
    ): OpinionDto

    @DeleteMapping(UNLINK_PATH)
    @Operation(
        summary = "Unlink component opinion",
        description = "Removes a component link from an opinion owned by the authenticated user.",
    )
    fun unlinkComponent(
        @Parameter(description = "Component link identifier.")
        @PathVariable linkId: UUID,
    ): OpinionDto

    @PatchMapping(ADJUST_WEIGHT_PATH)
    @Operation(
        summary = "Adjust component weight",
        description = "Changes the weight assigned to an existing component opinion link.",
    )
    fun adjustComponentWeight(
        @Parameter(description = "Component link identifier.")
        @PathVariable linkId: UUID,
        @Parameter(description = "Replacement component weight.")
        @RequestParam(required = true)
        weight: Double,
    ): OpinionDto
}
