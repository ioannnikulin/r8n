package com.r8n.backend.opinions.integration.client

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.api.lists.OpinionListsSearchApi
import com.r8n.backend.opinions.api.lists.OpinionListsSearchApi.Companion.APPROVED_PATH
import com.r8n.backend.opinions.api.lists.OpinionListsSearchApi.Companion.MINE_NAMES_PATH
import com.r8n.backend.opinions.api.lists.OpinionListsSearchApi.Companion.MINE_PATH
import com.r8n.backend.opinions.api.lists.OpinionListsSearchApi.Companion.SEARCH_PATH
import com.r8n.backend.opinions.api.lists.dto.OpinionListNameAndOwnerDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListNameDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListSearchFiltersDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListSummaryDto
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.util.Optional
import java.util.UUID

class OpinionListSearchRestClient(
    private val restClient: RestClient,
) : OpinionListsSearchApi {
    override fun discover(
        filters: OpinionListSearchFiltersDto,
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionListSummaryDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(SEARCH_PATH)
                    .queryParamIfPresent("nameSubstring", Optional.ofNullable(filters.nameSubstring))
                    .queryParamIfPresent("authorId", Optional.ofNullable(filters.authorId))
                    .queryParamIfPresent("authorNameSubstring", Optional.ofNullable(filters.authorNameSubstring))
                    .queryParamIfPresent("containsLocationSubstring", Optional.ofNullable(filters.containsLocationSubstring))
                    .queryParamIfPresent("someOpinionsYoungerThan", Optional.ofNullable(filters.someOpinionsYoungerThan))
                    .queryParamIfPresent("containsSubjectSubstring", Optional.ofNullable(filters.containsSubjectSubstring))
                    .queryParamIfPresent("findThisTextInAnyOfTheAbove", Optional.ofNullable(filters.findThisTextInAnyOfTheAbove))
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build()
            }.retrieve()
            .body<PageResponseDto<OpinionListSummaryDto>>()!!

    override fun getMine(pageable: PageRequestDto): PageResponseDto<OpinionListSummaryDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(MINE_PATH)
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build()
            }.retrieve()
            .body<PageResponseDto<OpinionListSummaryDto>>()!!

    override fun getApprovedListsWithNamesAndOwners(pageable: PageRequestDto): PageResponseDto<OpinionListNameAndOwnerDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(APPROVED_PATH)
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build()
            }.retrieve()
            .body<PageResponseDto<OpinionListNameAndOwnerDto>>()!!

    override fun getMineNamesOnly(pageable: PageRequestDto): PageResponseDto<OpinionListNameDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(MINE_NAMES_PATH)
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build()
            }.retrieve()
            .body<PageResponseDto<OpinionListNameDto>>()!!
}
