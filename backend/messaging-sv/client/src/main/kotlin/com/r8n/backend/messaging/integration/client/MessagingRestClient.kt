package com.r8n.backend.messaging.integration.client

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.messaging.api.MessagingApi
import com.r8n.backend.messaging.api.MessagingApi.Companion.DIRECT_CONVERSATIONS_PATH
import com.r8n.backend.messaging.api.MessagingApi.Companion.DIRECT_CONVERSATION_MESSAGES_PATH
import com.r8n.backend.messaging.api.MessagingApi.Companion.DIRECT_CONVERSATION_READ_PATH
import com.r8n.backend.messaging.api.MessagingApi.Companion.SUPPORT_THREADS_PATH
import com.r8n.backend.messaging.api.MessagingApi.Companion.SUPPORT_THREAD_MESSAGES_PATH
import com.r8n.backend.messaging.api.MessagingApi.Companion.SUPPORT_THREAD_PATH
import com.r8n.backend.messaging.api.MessagingApi.Companion.UNREAD_COUNT_PATH
import com.r8n.backend.messaging.api.dto.messaging.CreateDirectConversationRequestDto
import com.r8n.backend.messaging.api.dto.messaging.CreateDirectMessageRequestDto
import com.r8n.backend.messaging.api.dto.messaging.CreateSupportMessageRequestDto
import com.r8n.backend.messaging.api.dto.messaging.CreateSupportThreadRequestDto
import com.r8n.backend.messaging.api.dto.messaging.DirectConversationSummaryDto
import com.r8n.backend.messaging.api.dto.messaging.DirectMessageDto
import com.r8n.backend.messaging.api.dto.messaging.SupportMessageDto
import com.r8n.backend.messaging.api.dto.messaging.SupportThreadSummaryDto
import com.r8n.backend.messaging.api.dto.messaging.UnreadMessagesCountDto
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.util.UUID

class MessagingRestClient(
    private val restClient: RestClient,
) : MessagingApi {
    override fun getUnreadMessagesCount(): UnreadMessagesCountDto =
        restClient
            .get()
            .uri(UNREAD_COUNT_PATH)
            .retrieve()
            .body<UnreadMessagesCountDto>()!!

    override fun getDirectConversationSummaries(
        pageable: PageRequestDto,
    ): PageResponseDto<DirectConversationSummaryDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(DIRECT_CONVERSATIONS_PATH)
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build()
            }.retrieve()
            .body<PageResponseDto<DirectConversationSummaryDto>>()!!

    override fun createDirectConversation(request: CreateDirectConversationRequestDto): DirectConversationSummaryDto =
        restClient
            .post()
            .uri(DIRECT_CONVERSATIONS_PATH)
            .body(request)
            .retrieve()
            .body<DirectConversationSummaryDto>()!!

    override fun getDirectConversationMessages(
        conversationId: UUID,
        pageable: PageRequestDto,
    ): PageResponseDto<DirectMessageDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(DIRECT_CONVERSATION_MESSAGES_PATH)
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build(conversationId)
            }.retrieve()
            .body<PageResponseDto<DirectMessageDto>>()!!

    override fun addDirectConversationMessage(
        conversationId: UUID,
        request: CreateDirectMessageRequestDto,
    ): DirectMessageDto =
        restClient
            .post()
            .uri(DIRECT_CONVERSATION_MESSAGES_PATH, conversationId)
            .body(request)
            .retrieve()
            .body<DirectMessageDto>()!!

    override fun markDirectConversationAsRead(conversationId: UUID) {
        restClient
            .post()
            .uri(DIRECT_CONVERSATION_READ_PATH, conversationId)
            .retrieve()
            .toBodilessEntity()
    }

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

    override fun deleteSupportThread(threadId: UUID) {
        restClient
            .delete()
            .uri(SUPPORT_THREAD_PATH, threadId)
            .retrieve()
            .toBodilessEntity()
    }

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
