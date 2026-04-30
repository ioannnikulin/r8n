package com.r8n.backend.opinions.integration.client

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.api.access.OutgoingAccessRequestApi
import com.r8n.backend.opinions.api.access.OutgoingAccessRequestApi.Companion.CANCEL_PATH
import com.r8n.backend.opinions.api.access.OutgoingAccessRequestApi.Companion.CREATE_PATH
import com.r8n.backend.opinions.api.access.OutgoingAccessRequestApi.Companion.GET_PATH
import com.r8n.backend.opinions.api.access.dto.AccessRequestDto
import com.r8n.backend.opinions.api.access.dto.RequestStatusEnumDto
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.time.Instant
import java.util.Optional
import java.util.UUID

class OutgoingAccessRequestRestClient(
    private val restClient: RestClient,
) : OutgoingAccessRequestApi {
    override fun get(
        forListId: UUID?,
        since: Instant?,
        status: RequestStatusEnumDto?,
        pageable: PageRequestDto,
    ): PageResponseDto<AccessRequestDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(GET_PATH)
                    .queryParamIfPresent("forListId", Optional.ofNullable(forListId))
                    .queryParamIfPresent("since", Optional.ofNullable(since))
                    .queryParamIfPresent("status", Optional.ofNullable(status))
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build()
            }.retrieve()
            .body<PageResponseDto<AccessRequestDto>>()!!

    override fun create(listId: UUID): AccessRequestDto =
        restClient
            .post()
            .uri(CREATE_PATH, listId)
            .retrieve()
            .body<AccessRequestDto>()!!

    override fun cancel(requestId: UUID): AccessRequestDto =
        restClient
            .post()
            .uri(CANCEL_PATH, requestId)
            .retrieve()
            .body<AccessRequestDto>()!!
}
