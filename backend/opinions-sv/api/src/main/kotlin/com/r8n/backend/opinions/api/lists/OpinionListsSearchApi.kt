package com.r8n.backend.opinions.api.lists

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListNameAndOwnerDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListNameDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListPrivacyEnumDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListSearchFiltersDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListSummaryDto
import jakarta.validation.Valid
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
import java.time.Instant
import java.util.UUID

@Validated
interface OpinionListsSearchApi {
    companion object {
        private const val ROOT_PATH = "/api/opinion-lists"
        const val SEARCH_PATH = "$ROOT_PATH/search"
        const val MINE_PATH = "$ROOT_PATH/mine"
        const val APPROVED_PATH = "$ROOT_PATH/approved"
        const val MINE_NAMES_PATH = "$MINE_PATH/names"
    }

    @GetMapping(SEARCH_PATH)
    fun discover(
        @Valid
        filters: OpinionListSearchFiltersDto,
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionListSummaryDto>

    @GetMapping(MINE_PATH)
    fun getMine(
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionListSummaryDto>

    @GetMapping(APPROVED_PATH)
    fun getApprovedListsWithNamesAndOwners(
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionListNameAndOwnerDto>

    @GetMapping(MINE_NAMES_PATH)
    fun getMineNamesOnly(
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionListNameDto>
}
