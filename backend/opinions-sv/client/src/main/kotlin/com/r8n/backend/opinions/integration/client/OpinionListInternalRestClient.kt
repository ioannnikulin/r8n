package com.r8n.backend.opinions.integration.client

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListDto
import com.r8n.backend.opinions.integration.api.OpinionListsInternalApi
import com.r8n.backend.opinions.integration.api.OpinionListsInternalApi.Companion.MINE_FULL_PATH
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

class OpinionListInternalRestClient(
    private val restClient: RestClient,
) : OpinionListsInternalApi {
    override fun getMineFull(pageable: PageRequestDto): PageResponseDto<OpinionListDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(MINE_FULL_PATH)
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build()
            }.retrieve()
            .body<PageResponseDto<OpinionListDto>>()!!
}
