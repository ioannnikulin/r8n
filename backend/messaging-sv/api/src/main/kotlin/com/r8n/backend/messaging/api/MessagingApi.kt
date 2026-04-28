package com.r8n.backend.messaging.api

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.messaging.api.dto.CreateDirectThreadRequestDto
import com.r8n.backend.messaging.api.dto.CreateSupportThreadRequestDto
import com.r8n.backend.messaging.api.dto.CreateThreadMessageRequestDto
import com.r8n.backend.messaging.api.dto.MessageDto
import com.r8n.backend.messaging.api.dto.ThreadSummaryDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

interface MessagingApi {
    companion object {
        private const val ROOT_PATH = "/api/messaging"
        const val THREADS_PATH = "$ROOT_PATH/threads"
        const val SUPPORT_THREADS_PATH = "$ROOT_PATH/support/threads"
        const val DIRECT_THREADS_PATH = "$ROOT_PATH/direct/threads"
        const val THREAD_MESSAGES_PATH = "$ROOT_PATH/threads/{threadId}/messages"
        const val THREAD_READ_PATH = "$ROOT_PATH/threads/{threadId}/read"
    }

    @GetMapping(THREADS_PATH)
    fun getThreads(pageable: PageRequestDto): PageResponseDto<ThreadSummaryDto>

    @GetMapping(THREAD_MESSAGES_PATH)
    fun getThreadMessages(
        @PathVariable threadId: UUID,
        pageable: PageRequestDto,
    ): PageResponseDto<MessageDto>

    @PostMapping(SUPPORT_THREADS_PATH)
    fun createSupportThread(
        @RequestBody request: CreateSupportThreadRequestDto,
    ): ThreadSummaryDto

    @PostMapping(DIRECT_THREADS_PATH)
    fun createDirectThread(
        @RequestBody request: CreateDirectThreadRequestDto,
    ): ThreadSummaryDto

    @PostMapping(THREAD_MESSAGES_PATH)
    fun addThreadMessage(
        @PathVariable threadId: UUID,
        @RequestBody request: CreateThreadMessageRequestDto,
    ): MessageDto

    @PostMapping(THREAD_READ_PATH)
    fun markThreadRead(
        @PathVariable threadId: UUID,
    ): ThreadSummaryDto
}