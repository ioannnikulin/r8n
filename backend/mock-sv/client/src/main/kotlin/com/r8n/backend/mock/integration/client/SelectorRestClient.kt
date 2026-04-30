package com.r8n.backend.mock.integration.client

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.mock.api.SelectorApi
import com.r8n.backend.mock.api.SelectorApi.Companion.FOR_SUBJECT_PATH
import com.r8n.backend.mock.api.SelectorApi.Companion.FOR_URL_PATH
import com.r8n.backend.mock.api.dto.about.SelectorDto
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.util.UUID

class SelectorRestClient(
    private val restClient: RestClient,
) : SelectorApi {
    override fun getForURL(
        url: String,
        pageable: PageRequestDto,
    ): PageResponseDto<SelectorDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(FOR_URL_PATH)
                    .queryParam("url", url)
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build()
            }.retrieve()
            .body<PageResponseDto<SelectorDto>>()!!

    override fun getForSubject(
        subjectId: UUID,
        pageable: PageRequestDto,
    ): PageResponseDto<SelectorDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(FOR_SUBJECT_PATH)
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build(subjectId)
            }.retrieve()
            .body<PageResponseDto<SelectorDto>>()!!

    override fun suggest(
        subjectId: UUID,
        selector: String,
    ): SelectorDto =
        restClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path(FOR_SUBJECT_PATH)
                    .queryParam("selector", selector)
                    .build(subjectId)
            }.retrieve()
            .body<SelectorDto>()!!
}
