package com.r8n.backend.mock.integration.client

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.mock.api.RecommendationApi
import com.r8n.backend.mock.api.RecommendationApi.Companion.HIDDEN_LISTS_PATH
import com.r8n.backend.mock.api.RecommendationApi.Companion.HIDDEN_SUBJECTS_PATH
import com.r8n.backend.mock.api.RecommendationApi.Companion.HIDE_LIST_PATH
import com.r8n.backend.mock.api.RecommendationApi.Companion.HIDE_SUBJECT_PATH
import com.r8n.backend.mock.api.RecommendationApi.Companion.OPINION_LISTS_PATH
import com.r8n.backend.mock.api.RecommendationApi.Companion.SUBJECTS_PATH
import com.r8n.backend.mock.api.RecommendationApi.Companion.UNHIDE_LIST_PATH
import com.r8n.backend.mock.api.RecommendationApi.Companion.UNHIDE_SUBJECT_PATH
import com.r8n.backend.opinions.api.lists.dto.OpinionListSummaryDto
import com.r8n.backend.opinions.api.opinions.dto.OpinionSubjectDto
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.util.UUID

class RecommendationRestClient(
    private val restClient: RestClient,
) : RecommendationApi {
    override fun getRecommendedSubjects(
        lookingAtSubjectId: UUID,
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionSubjectDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(SUBJECTS_PATH)
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build(lookingAtSubjectId)
            }.retrieve()
            .body<PageResponseDto<OpinionSubjectDto>>()!!

    override fun getRecommendedOpinionLists(
        lookingAtListId: UUID,
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionListSummaryDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(OPINION_LISTS_PATH)
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build(lookingAtListId)
            }.retrieve()
            .body<PageResponseDto<OpinionListSummaryDto>>()!!

    override fun hideRecommendedSubject(id: UUID) {
        restClient
            .patch()
            .uri(HIDE_SUBJECT_PATH, id)
            .retrieve()
            .toBodilessEntity()
    }

    override fun hideRecommendedOpinionList(id: UUID) {
        restClient
            .patch()
            .uri(HIDE_LIST_PATH, id)
            .retrieve()
            .toBodilessEntity()
    }

    override fun getHiddenSubjects(pageable: PageRequestDto): PageResponseDto<OpinionSubjectDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(HIDDEN_SUBJECTS_PATH)
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build()
            }.retrieve()
            .body<PageResponseDto<OpinionSubjectDto>>()!!

    override fun getHiddenOpinionLists(pageable: PageRequestDto): PageResponseDto<OpinionListSummaryDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(HIDDEN_LISTS_PATH)
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build()
            }.retrieve()
            .body<PageResponseDto<OpinionListSummaryDto>>()!!

    override fun unhideSubject(id: UUID): OpinionSubjectDto =
        restClient
            .patch()
            .uri(UNHIDE_SUBJECT_PATH, id)
            .retrieve()
            .body<OpinionSubjectDto>()!!

    override fun unhideOpinionList(id: UUID): OpinionListSummaryDto =
        restClient
            .patch()
            .uri(UNHIDE_LIST_PATH, id)
            .retrieve()
            .body<OpinionListSummaryDto>()!!
}
