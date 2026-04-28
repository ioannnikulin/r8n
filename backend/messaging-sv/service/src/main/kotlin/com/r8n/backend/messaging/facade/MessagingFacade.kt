package com.r8n.backend.messaging.facade

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.core.utils.toPageable
import com.r8n.backend.core.utils.toResponse
import com.r8n.backend.messaging.api.dto.CreateDirectThreadRequestDto
import com.r8n.backend.messaging.api.dto.CreateSupportThreadRequestDto
import com.r8n.backend.messaging.api.dto.CreateThreadMessageRequestDto
import com.r8n.backend.messaging.api.dto.MessageAuthorRoleEnumDto
import com.r8n.backend.messaging.api.dto.MessageDto
import com.r8n.backend.messaging.api.dto.ThreadParticipantDto
import com.r8n.backend.messaging.api.dto.ThreadSummaryDto
import com.r8n.backend.messaging.api.dto.ThreadTypeEnumDto
import com.r8n.backend.messaging.domain.MessageAuthorRoleEnum
import com.r8n.backend.messaging.domain.MessagingThread
import com.r8n.backend.messaging.domain.ThreadMessage
import com.r8n.backend.messaging.domain.ThreadTypeEnum
import com.r8n.backend.messaging.service.MessagingService
import com.r8n.backend.users.integration.api.UsersInternalApi
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MessagingFacade(
    private val messagingService: MessagingService,
    private val usersClient: UsersInternalApi,
) {
    fun getThreads(
        requesterId: UUID,
        pageable: PageRequestDto,
    ): PageResponseDto<ThreadSummaryDto> {
        val usernameCache = mutableMapOf<UUID, String>()
        return messagingService
            .getVisibleThreads(requesterId, pageable.toPageable())
            .map { it.toDto(requesterId, usernameCache) }
            .toResponse()
    }

    fun getThreadMessages(
        requesterId: UUID,
        threadId: UUID,
        pageable: PageRequestDto,
    ): PageResponseDto<MessageDto> {
        val usernameCache = mutableMapOf<UUID, String>()
        return messagingService
            .getMessages(requesterId, threadId, pageable.toPageable())
            .map { it.toDto(requesterId, usernameCache) }
            .toResponse()
    }

    fun createSupportThread(
        requesterId: UUID,
        request: CreateSupportThreadRequestDto,
    ): ThreadSummaryDto =
        messagingService
            .createSupportThread(requesterId, request.initialMessage)
            .toDto(requesterId, mutableMapOf())

    fun createDirectThread(
        requesterId: UUID,
        request: CreateDirectThreadRequestDto,
    ): ThreadSummaryDto =
        messagingService
            .createDirectThread(requesterId, request.recipientUserId, request.initialMessage)
            .toDto(requesterId, mutableMapOf())

    fun addThreadMessage(
        requesterId: UUID,
        threadId: UUID,
        request: CreateThreadMessageRequestDto,
    ): MessageDto =
        messagingService
            .sendMessage(requesterId, threadId, request.text)
            .toDto(requesterId, mutableMapOf())

    fun markThreadRead(
        requesterId: UUID,
        threadId: UUID,
    ): ThreadSummaryDto =
        messagingService
            .markRead(requesterId, threadId)
            .toDto(requesterId, mutableMapOf())

    private fun MessagingThread.toDto(
        requesterId: UUID,
        usernameCache: MutableMap<UUID, String>,
    ): ThreadSummaryDto =
        ThreadSummaryDto(
            id = id,
            type = type.toDto(),
            participant = participant(requesterId, usernameCache),
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastMessagePreview = lastMessage?.text.orEmpty(),
            lastMessageOwn = lastMessage?.authorUserId == requesterId,
            unreadCount = unreadCount,
        )

    private fun MessagingThread.participant(
        requesterId: UUID,
        usernameCache: MutableMap<UUID, String>,
    ): ThreadParticipantDto =
        if (type == ThreadTypeEnum.SUPPORT) {
            ThreadParticipantDto(
                userId = null,
                name = SUPPORT_NAME,
                role = MessageAuthorRoleEnumDto.SUPPORT,
            )
        } else {
            val participantUserId = if (requesterUserId == requesterId) recipientUserId!! else requesterUserId
            ThreadParticipantDto(
                userId = participantUserId,
                name = username(participantUserId, usernameCache),
                role = MessageAuthorRoleEnumDto.USER,
            )
        }

    private fun ThreadMessage.toDto(
        requesterId: UUID,
        usernameCache: MutableMap<UUID, String>,
    ): MessageDto =
        MessageDto(
            id = id,
            threadId = threadId,
            authorUserId = authorUserId,
            authorName = authorName(usernameCache),
            authorRole = authorRole.toDto(),
            own = authorUserId == requesterId,
            text = text,
            createdAt = createdAt,
        )

    private fun ThreadMessage.authorName(usernameCache: MutableMap<UUID, String>): String =
        if (authorRole == MessageAuthorRoleEnum.SUPPORT) {
            SUPPORT_NAME
        } else {
            username(authorUserId, usernameCache)
        }

    private fun username(
        userId: UUID,
        usernameCache: MutableMap<UUID, String>,
    ): String = usernameCache.getOrPut(userId) { usersClient.getUserName(userId) }

    private fun ThreadTypeEnum.toDto(): ThreadTypeEnumDto =
        when (this) {
            ThreadTypeEnum.SUPPORT -> ThreadTypeEnumDto.SUPPORT
            ThreadTypeEnum.DIRECT -> ThreadTypeEnumDto.DIRECT
        }

    private fun MessageAuthorRoleEnum.toDto(): MessageAuthorRoleEnumDto =
        when (this) {
            MessageAuthorRoleEnum.USER -> MessageAuthorRoleEnumDto.USER
            MessageAuthorRoleEnum.SUPPORT -> MessageAuthorRoleEnumDto.SUPPORT
        }

    private companion object {
        const val SUPPORT_NAME = "R8N Support"
    }
}