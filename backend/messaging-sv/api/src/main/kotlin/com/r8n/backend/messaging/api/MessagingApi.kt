package com.r8n.backend.messaging.api

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.messaging.api.dto.messaging.CreateDirectConversationRequestDto
import com.r8n.backend.messaging.api.dto.messaging.CreateDirectMessageRequestDto
import com.r8n.backend.messaging.api.dto.messaging.CreateSupportMessageRequestDto
import com.r8n.backend.messaging.api.dto.messaging.CreateSupportThreadRequestDto
import com.r8n.backend.messaging.api.dto.messaging.DirectConversationSummaryDto
import com.r8n.backend.messaging.api.dto.messaging.DirectMessageDto
import com.r8n.backend.messaging.api.dto.messaging.SupportMessageDto
import com.r8n.backend.messaging.api.dto.messaging.SupportThreadSummaryDto
import com.r8n.backend.messaging.api.dto.messaging.UnreadMessagesCountDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.UUID

@Tag(
    name = "Messaging",
    description = "Direct conversations between users and support conversations with the support team.",
)
interface MessagingApi {
    companion object {
        private const val ROOT_PATH = "/api/messaging"
        const val UNREAD_COUNT_PATH = "$ROOT_PATH/unread-count"
        const val SUPPORT_PATH = "$ROOT_PATH/support"
        const val SUPPORT_THREADS_PATH = "$SUPPORT_PATH/threads"
        const val SUPPORT_THREAD_PATH = "$SUPPORT_THREADS_PATH/{threadId}"
        const val SUPPORT_THREAD_MESSAGES_PATH = "$SUPPORT_THREADS_PATH/{threadId}/messages"
        const val DIRECT_PATH = "$ROOT_PATH/direct"
        const val DIRECT_CONVERSATIONS_PATH = "$DIRECT_PATH/conversations"
        const val DIRECT_CONVERSATION_PATH = "$DIRECT_CONVERSATIONS_PATH/{conversationId}"
        const val DIRECT_CONVERSATION_MESSAGES_PATH = "$DIRECT_CONVERSATION_PATH/messages"
        const val DIRECT_CONVERSATION_READ_PATH = "$DIRECT_CONVERSATION_PATH/read"
    }

    @GetMapping(UNREAD_COUNT_PATH)
    @Operation(
        summary = "Get unread message count",
        description = "Returns the authenticated actor's aggregate unread message count across messaging surfaces.",
    )
    fun getUnreadMessagesCount(): UnreadMessagesCountDto

    @GetMapping(DIRECT_CONVERSATIONS_PATH)
    fun getDirectConversationSummaries(
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<DirectConversationSummaryDto>

    @PostMapping(DIRECT_CONVERSATIONS_PATH)
    fun createDirectConversation(
        @Valid
        @RequestBody
        request: CreateDirectConversationRequestDto,
    ): DirectConversationSummaryDto

    @GetMapping(DIRECT_CONVERSATION_MESSAGES_PATH)
    fun getDirectConversationMessages(
        @PathVariable
        conversationId: UUID,
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<DirectMessageDto>

    @PostMapping(DIRECT_CONVERSATION_MESSAGES_PATH)
    fun addDirectConversationMessage(
        @PathVariable
        conversationId: UUID,
        @Valid
        @RequestBody
        request: CreateDirectMessageRequestDto,
    ): DirectMessageDto

    @PostMapping(DIRECT_CONVERSATION_READ_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markDirectConversationAsRead(
        @PathVariable
        conversationId: UUID,
    )

    @GetMapping(SUPPORT_THREADS_PATH)
    @Operation(
        summary = "List support threads",
        description = "Returns the authenticated actor's support thread summaries, ordered and paged by the service.",
    )
    fun getSupportThreadSummaries(
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<SupportThreadSummaryDto>

    @PostMapping(SUPPORT_THREADS_PATH)
    @Operation(
        summary = "Create a support thread",
        description = "Starts a new support thread for the authenticated user and stores the initial message.",
    )
    fun createSupportThread(
        @Valid
        @RequestBody
        request: CreateSupportThreadRequestDto,
    ): SupportThreadSummaryDto

    @GetMapping(SUPPORT_THREAD_MESSAGES_PATH)
    @Operation(
        summary = "List support thread messages",
        description = "Returns paged messages for a support thread visible to the authenticated actor.",
    )
    fun getSupportThreadMessages(
        @Parameter(description = "Support thread identifier.")
        @PathVariable
        threadId: UUID,
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<SupportMessageDto>

    @DeleteMapping(SUPPORT_THREAD_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteSupportThread(
        @PathVariable
        threadId: UUID,
    )

    @PostMapping(SUPPORT_THREAD_MESSAGES_PATH)
    @Operation(
        summary = "Add a support message",
        description = "Adds a message to an existing support thread visible to the authenticated actor.",
    )
    fun addSupportThreadMessage(
        @Parameter(description = "Support thread identifier.")
        @PathVariable
        threadId: UUID,
        @Valid
        @RequestBody
        request: CreateSupportMessageRequestDto,
    ): SupportMessageDto
}
