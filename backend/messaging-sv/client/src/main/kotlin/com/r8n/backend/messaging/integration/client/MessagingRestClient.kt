package com.r8n.backend.messaging.integration.client

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.messaging.api.MessagingApi
import com.r8n.backend.messaging.api.MessagingApi.Companion.SUPPORT_THREADS_PATH
import com.r8n.backend.messaging.api.MessagingApi.Companion.SUPPORT_THREAD_MESSAGES_PATH
import com.r8n.backend.messaging.api.dto.messaging.CreateSupportMessageRequestDto
import com.r8n.backend.messaging.api.dto.messaging.CreateSupportThreadRequestDto
import com.r8n.backend.messaging.api.dto.messaging.SupportMessageDto
import com.r8n.backend.messaging.api.dto.messaging.SupportThreadSummaryDto
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.util.UUID

class MessagingRestClient(
    private val restClient: RestClient,
) : MessagingApi {
    override fun getSupportThreadSummaries(pageable: PageRequestDto): PageResponseDto<SupportThreadSummaryDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(SUPPORT_THREADS_PATH)
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build()
            }.retrieve()
            .body<PageResponseDto<SupportThreadSummaryDto>>()!!

    override fun createSupportThread(request: CreateSupportThreadRequestDto): SupportThreadSummaryDto =
        restClient
            .post()
            .uri(SUPPORT_THREADS_PATH)
            .body(request)
            .retrieve()
            .body<SupportThreadSummaryDto>()!!

    override fun getSupportThreadMessages(
        threadId: UUID,
        pageable: PageRequestDto,
    ): PageResponseDto<SupportMessageDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(SUPPORT_THREAD_MESSAGES_PATH)
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build(threadId)
            }.retrieve()
            .body<PageResponseDto<SupportMessageDto>>()!!

    override fun addSupportThreadMessage(
        threadId: UUID,
        request: CreateSupportMessageRequestDto,
    ): SupportMessageDto =
        restClient
            .post()
            .uri(SUPPORT_THREAD_MESSAGES_PATH, threadId)
            .body(request)
            .retrieve()
            .body<SupportMessageDto>()!!
}
