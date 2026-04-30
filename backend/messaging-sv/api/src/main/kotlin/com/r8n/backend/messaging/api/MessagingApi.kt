package com.r8n.backend.messaging.api

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.messaging.api.dto.messaging.CreateSupportMessageRequestDto
import com.r8n.backend.messaging.api.dto.messaging.CreateSupportThreadRequestDto
import com.r8n.backend.messaging.api.dto.messaging.SupportMessageDto
import com.r8n.backend.messaging.api.dto.messaging.SupportThreadSummaryDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@Tag(name = "Support messaging", description = "Support conversations between users and the support team.")
interface MessagingApi {
    companion object {
        private const val ROOT_PATH = "/api/messaging"
        const val SUPPORT_PATH = "$ROOT_PATH/support"
        const val SUPPORT_THREADS_PATH = "$SUPPORT_PATH/threads"
        const val SUPPORT_THREAD_MESSAGES_PATH = "$SUPPORT_THREADS_PATH/{threadId}/messages"
    }

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
