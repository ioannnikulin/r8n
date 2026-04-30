package com.r8n.backend.opinions.api.lists

import com.r8n.backend.opinions.api.lists.dto.OpinionListDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListPrivacyEnumDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListSummaryDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Validated
@Tag(name = "Opinion lists", description = "Opinion-list ownership, privacy, and opinion linking endpoints.")
interface OpinionListsApi {
    companion object {
        private const val ROOT_PATH = "/api/opinion-lists"
        const val SUMMARY_PATH = "$ROOT_PATH/{listId}/summary"
        const val GET_PATH = "$ROOT_PATH/{listId}"
        const val CREATE_PATH = ROOT_PATH
        const val RENAME_PATH = "$ROOT_PATH/{listId}/rename"
        const val SET_PRIVACY_PATH = "$ROOT_PATH/{listId}/set-privacy"
        const val LINK_PATH = "$ROOT_PATH/{listId}/link"
        const val UNLINK_PATH = "$ROOT_PATH/{listId}/unlink"
        const val SYNC_PATH = "$ROOT_PATH/{existingListId}/sync"
        const val UNSYNC_PATH = "$ROOT_PATH/{existingListId}/unsync"
        const val DELETE_PATH = "$ROOT_PATH/{listId}"
        const val MOVE_OPINION_PATH = "$ROOT_PATH/{fromListId}/move-opinion"
    }

    @GetMapping(SUMMARY_PATH)
    @Operation(
        summary = "Get opinion list summary",
        description = "Returns summary information for an opinion list visible to the authenticated user.",
    )
    fun getListSummary(
        @Parameter(description = "Opinion list identifier.")
        @PathVariable listId: UUID,
    ): OpinionListSummaryDto

    @GetMapping(GET_PATH)
    @Operation(
        summary = "Get opinion list",
        description = "Returns an opinion list visible to the authenticated user.",
    )
    fun getList(
        @Parameter(description = "Opinion list identifier.")
        @PathVariable listId: UUID,
        @Parameter(description = "Only include opinions published after this timestamp.")
        @RequestParam(required = false) publishedAfter: java.time.Instant?,
    ): OpinionListDto

    @PostMapping(CREATE_PATH)
    @Operation(
        summary = "Create opinion list",
        description = "Creates an opinion list owned by the authenticated user.",
    )
    fun createList(
        @Parameter(description = "Opinion list name.")
        @RequestParam(required = true)
        @NotBlank
        @Size(min = 1, max = 255)
        name: String,
        @Parameter(description = "Initial opinion list privacy setting.")
        @RequestParam(required = true)
        privacy: OpinionListPrivacyEnumDto,
    ): OpinionListDto

    @PatchMapping(RENAME_PATH)
    @Operation(
        summary = "Rename opinion list",
        description = "Renames an opinion list owned by the authenticated user.",
    )
    fun renameList(
        @Parameter(description = "Opinion list identifier.")
        @PathVariable
        listId: UUID,
        @Parameter(description = "Replacement opinion list name.")
        @RequestParam(required = true)
        @NotBlank
        @Size(min = 1, max = 255)
        name: String,
    ): OpinionListDto

    @PatchMapping(SET_PRIVACY_PATH)
    @Operation(
        summary = "Change opinion list privacy",
        description = "Changes the privacy setting for an opinion list owned by the authenticated user.",
    )
    fun changePrivacy(
        @Parameter(description = "Opinion list identifier.")
        @PathVariable
        listId: UUID,
        @Parameter(description = "Replacement privacy setting.")
        @RequestParam(required = true)
        privacy: OpinionListPrivacyEnumDto,
    ): OpinionListDto

    @DeleteMapping(DELETE_PATH)
    @Operation(
        summary = "Delete opinion list",
        description = "Deletes an opinion list owned by the authenticated user.",
    )
    fun deleteList(
        @Parameter(description = "Opinion list identifier.")
        @PathVariable
        listId: UUID,
    )

    @PostMapping(MOVE_OPINION_PATH)
    @Operation(
        summary = "Move linked opinion",
        description = "Moves an opinion link from one opinion list to another and sets its weight.",
    )
    fun moveOpinion(
        @Parameter(description = "Source opinion list identifier.")
        @PathVariable
        fromListId: UUID,
        @Parameter(description = "Target opinion list identifier.")
        @RequestParam(required = true)
        toListId: UUID,
        @Parameter(description = "Opinion identifier to move.")
        @RequestParam(required = true)
        opinionId: UUID,
        @Parameter(description = "Opinion weight in the target list.")
        @RequestParam(defaultValue = "1.0")
        @Min(0)
        @Max(1)
        weight: Double,
    ): OpinionListDto

    @PostMapping(LINK_PATH)
    @Operation(
        summary = "Link opinion to list",
        description = "Adds an opinion to an opinion list owned by the authenticated user.",
    )
    fun linkOpinion(
        @Parameter(description = "Opinion list identifier.")
        @PathVariable
        listId: UUID,
        @Parameter(description = "Opinion identifier to link.")
        @RequestParam(required = true)
        opinionId: UUID,
        @Parameter(description = "Opinion weight in the list.")
        @RequestParam(defaultValue = "1.0")
        @Min(0)
        @Max(1)
        weight: Double,
    ): OpinionListDto

    @PatchMapping(UNLINK_PATH)
    @Operation(
        summary = "Unlink opinion from list",
        description = "Removes an opinion from an opinion list owned by the authenticated user.",
    )
    fun unlinkOpinion(
        @Parameter(description = "Opinion list identifier.")
        @PathVariable
        listId: UUID,
        @Parameter(description = "Opinion identifier to unlink.")
        @RequestParam(required = true)
        opinionId: UUID,
    ): OpinionListDto

    @PostMapping(SYNC_PATH)
    @Operation(
        summary = "Sync opinion list",
        description = "Links another opinion list as a weighted source for an existing opinion list.",
    )
    fun syncWithOpinionList(
        @Parameter(description = "Opinion list receiving the synced source.")
        @PathVariable
        existingListId: UUID,
        @Parameter(description = "Opinion list to add as a synced source.")
        @RequestParam(required = true)
        addedListId: UUID,
        @Parameter(description = "Synced list weight.")
        @RequestParam(defaultValue = "1.0")
        @Min(0)
        @Max(1)
        weight: Double,
    ): OpinionListDto

    @PostMapping(UNSYNC_PATH)
    @Operation(
        summary = "Unsync opinion list",
        description = "Removes a synced opinion list source from an existing opinion list.",
    )
    fun unsyncWithOpinionList(
        @Parameter(description = "Opinion list that contains the synced source.")
        @PathVariable
        existingListId: UUID,
        @Parameter(description = "Synced opinion list to remove.")
        @RequestParam(required = true)
        removedListId: UUID,
    ): OpinionListDto
}
