package com.r8n.backend.opinions.api.lists

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListNameAndOwnerDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListNameDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListSearchFiltersDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListSummaryDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping

@Validated
@Tag(
    name = "Opinion list search",
    description = "Opinion-list discovery and authenticated user's list lookup endpoints.",
)
interface OpinionListsSearchApi {
    companion object {
        private const val ROOT_PATH = "/api/opinion-lists"
        const val SEARCH_PATH = "$ROOT_PATH/search"
        const val MINE_PATH = "$ROOT_PATH/mine"
        const val APPROVED_PATH = "$ROOT_PATH/approved"
        const val MINE_NAMES_PATH = "$MINE_PATH/names"
    }

    @GetMapping(SEARCH_PATH)
    @Operation(
        summary = "Discover opinion lists",
        description = "Searches opinion lists visible to the authenticated user with filter and paging parameters.",
    )
    fun discover(
        @Valid
        filters: OpinionListSearchFiltersDto,
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionListSummaryDto>

    @GetMapping(MINE_PATH)
    @Operation(
        summary = "List my opinion lists",
        description = "Returns opinion lists owned by the authenticated user.",
    )
    fun getMine(
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionListSummaryDto>

    @GetMapping(APPROVED_PATH)
    @Operation(
        summary = "List approved opinion lists",
        description = "Returns approved opinion lists with names and owner information.",
    )
    fun getApprovedListsWithNamesAndOwners(
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionListNameAndOwnerDto>

    @GetMapping(MINE_NAMES_PATH)
    @Operation(
        summary = "List my opinion list names",
        description = "Returns only names and identifiers for opinion lists owned by the authenticated user.",
    )
    fun getMineNamesOnly(
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionListNameDto>
}
