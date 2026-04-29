package com.r8n.backend.opinions.api.lists

import com.r8n.backend.opinions.api.lists.dto.OpinionListDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListPrivacyEnumDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListSummaryDto
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Validated
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
    }

    @GetMapping(SUMMARY_PATH)
    fun getListSummary(
        @PathVariable listId: UUID,
    ): OpinionListSummaryDto

    @GetMapping(GET_PATH)
    fun getList(
        @PathVariable listId: UUID,
        @RequestParam(required = false) publishedAfter: java.time.Instant?,
    ): OpinionListDto

    @PostMapping(CREATE_PATH)
    fun createList(
        @RequestParam(required = true)
        @NotBlank
        @Size(min = 1, max = 255)
        name: String,
        @RequestParam(required = true)
        privacy: OpinionListPrivacyEnumDto,
    ): OpinionListDto

    @PatchMapping(RENAME_PATH)
    fun renameList(
        @PathVariable
        listId: UUID,
        @RequestParam(required = true)
        @NotBlank
        @Size(min = 1, max = 255)
        name: String,
    ): OpinionListDto

    @PatchMapping(SET_PRIVACY_PATH)
    fun changePrivacy(
        @PathVariable
        listId: UUID,
        @RequestParam(required = true)
        privacy: OpinionListPrivacyEnumDto,
    ): OpinionListDto

    @PostMapping(LINK_PATH)
    fun linkOpinion(
        @PathVariable
        listId: UUID,
        @RequestParam(required = true)
        opinionId: UUID,
        @RequestParam(defaultValue = "1.0")
        @Min(0)
        @Max(1)
        weight: Double,
    ): OpinionListDto

    @PatchMapping(UNLINK_PATH)
    fun unlinkOpinion(
        @PathVariable
        listId: UUID,
        @RequestParam(required = true)
        opinionId: UUID,
    ): OpinionListDto

    @PostMapping(SYNC_PATH)
    fun syncWithOpinionList(
        @PathVariable
        existingListId: UUID,
        @RequestParam(required = true)
        addedListId: UUID,
        @RequestParam(defaultValue = "1.0")
        @Min(0)
        @Max(1)
        weight: Double,
    ): OpinionListDto

    @PostMapping(UNSYNC_PATH)
    fun unsyncWithOpinionList(
        @PathVariable
        existingListId: UUID,
        @RequestParam(required = true)
        removedListId: UUID,
    ): OpinionListDto
}
