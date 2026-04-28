package com.r8n.backend.messaging.controller

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.messaging.api.MessagingApi
import com.r8n.backend.messaging.api.dto.CreateDirectThreadRequestDto
import com.r8n.backend.messaging.api.dto.CreateSupportThreadRequestDto
import com.r8n.backend.messaging.api.dto.CreateThreadMessageRequestDto
import com.r8n.backend.messaging.api.dto.MessageDto
import com.r8n.backend.messaging.api.dto.ThreadSummaryDto
import com.r8n.backend.messaging.facade.MessagingFacade
import com.r8n.backend.security.Authority.IS_USER
import com.r8n.backend.security.CurrentUserIdentifier.getCurrentUserId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class MessagingController(
    private val messagingFacade: MessagingFacade,
) : MessagingApi {
    @PreAuthorize(IS_USER)
    override fun getThreads(pageable: PageRequestDto): PageResponseDto<ThreadSummaryDto> =
        messagingFacade.getThreads(getCurrentUserId(), pageable)

    @PreAuthorize(IS_USER)
    override fun getThreadMessages(
        threadId: UUID,
        pageable: PageRequestDto,
    ): PageResponseDto<MessageDto> = messagingFacade.getThreadMessages(getCurrentUserId(), threadId, pageable)

    @PreAuthorize(IS_USER)
    override fun createSupportThread(request: CreateSupportThreadRequestDto): ThreadSummaryDto =
        messagingFacade.createSupportThread(getCurrentUserId(), request)

    @PreAuthorize(IS_USER)
    override fun createDirectThread(request: CreateDirectThreadRequestDto): ThreadSummaryDto =
        messagingFacade.createDirectThread(getCurrentUserId(), request)

    @PreAuthorize(IS_USER)
    override fun addThreadMessage(
        threadId: UUID,
        request: CreateThreadMessageRequestDto,
    ): MessageDto = messagingFacade.addThreadMessage(getCurrentUserId(), threadId, request)

    @PreAuthorize(IS_USER)
    override fun markThreadRead(threadId: UUID): ThreadSummaryDto =
        messagingFacade.markThreadRead(getCurrentUserId(), threadId)
}