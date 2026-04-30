package com.r8n.backend.messaging.facade

import com.r8n.backend.messaging.api.dto.messaging.CreateSupportMessageRequestDto
import com.r8n.backend.messaging.api.dto.messaging.CreateSupportThreadRequestDto
import com.r8n.backend.messaging.api.dto.messaging.SupportMessageDto
import com.r8n.backend.messaging.api.dto.messaging.SupportParticipantRoleEnumDto
import com.r8n.backend.messaging.api.dto.messaging.SupportThreadSummaryDto
import com.r8n.backend.messaging.api.dto.messaging.SupportThreadViewerRoleEnumDto
import com.r8n.backend.messaging.persistence.SupportMessagePersistence
import com.r8n.backend.messaging.persistence.SupportParticipantRoleEnumPersistence
import com.r8n.backend.messaging.persistence.SupportThreadPersistence
import com.r8n.backend.messaging.service.SupportActor
import com.r8n.backend.messaging.service.SupportMessagingService
import com.r8n.backend.messaging.service.SupportThreadSummary
import com.r8n.backend.messaging.service.SupportThreadWithMessages
import com.r8n.backend.users.integration.api.UsersInternalApi
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SupportMessagingFacade(
    private val supportMessagingService: SupportMessagingService,
    private val usersInternalApi: UsersInternalApi,
) {
    fun getSupportThreadSummaries(
        actor: SupportActor,
        pageable: Pageable,
    ): Page<SupportThreadSummaryDto> =
        supportMessagingService.listThreadSummaries(actor, pageable).map { it.toDto(actor) }

    fun createSupportThread(
        actor: SupportActor,
        request: CreateSupportThreadRequestDto,
    ): SupportThreadSummaryDto =
        supportMessagingService.createThread(actor, request.initialMessage.trim()).toSummaryDto(actor)

    fun getSupportThreadMessages(
        actor: SupportActor,
        threadId: UUID,
        pageable: Pageable,
    ): Page<SupportMessageDto> {
        val authorNames = mutableMapOf<UUID, String>()
        return supportMessagingService.getThreadMessages(actor, threadId, pageable).map {
            val authorDisplayName =
                authorNames.getOrPut(it.authorUserId) {
                    usersInternalApi.getUserName(it.authorUserId)
                }
            it.toDto(authorDisplayName)
        }
    }

    fun deleteSupportThread(
        actor: SupportActor,
        threadId: UUID,
    ) = supportMessagingService.deleteThread(actor, threadId)

    fun addSupportThreadMessage(
        actor: SupportActor,
        threadId: UUID,
        request: CreateSupportMessageRequestDto,
    ): SupportMessageDto {
        val message = supportMessagingService.addThreadMessage(actor, threadId, request.text.trim())
        return message.toDto(usersInternalApi.getUserName(message.authorUserId))
    }

    fun countUnreadMessages(actor: SupportActor): Long = supportMessagingService.countUnreadMessages(actor)

    private fun SupportThreadWithMessages.toSummaryDto(actor: SupportActor): SupportThreadSummaryDto =
        SupportThreadSummaryDto(
            id = requireNotNull(thread.id),
            ownerUserId = thread.ownerUserId,
            viewerRole = thread.viewerRoleFor(actor),
            createdAt = requireNotNull(messages.minOfOrNull { it.createdAt }),
            lastMessageAt = messages.maxOfOrNull { it.createdAt },
            lastMessageText = messages.maxByOrNull { it.createdAt }?.text,
            unreadCount = 0,
        )

    private fun SupportThreadSummary.toDto(actor: SupportActor): SupportThreadSummaryDto =
        SupportThreadSummaryDto(
            id = id,
            ownerUserId = ownerUserId,
            viewerRole = viewerRoleFor(actor),
            createdAt = createdAt,
            lastMessageAt = lastMessageAt,
            lastMessageText = lastMessageText,
            unreadCount = unreadCount,
        )

    private fun SupportMessagePersistence.toDto(authorDisplayName: String): SupportMessageDto =
        SupportMessageDto(
            id = requireNotNull(id),
            threadId = threadId,
            authorUserId = authorUserId,
            authorDisplayName = authorDisplayName,
            authorRole = authorRole.toDto(),
            text = text,
            createdAt = createdAt,
        )

    private fun SupportParticipantRoleEnumPersistence.toDto(): SupportParticipantRoleEnumDto =
        when (this) {
            SupportParticipantRoleEnumPersistence.USER -> SupportParticipantRoleEnumDto.USER
            SupportParticipantRoleEnumPersistence.SUPPORT -> SupportParticipantRoleEnumDto.SUPPORT
        }

    private fun SupportThreadPersistence.viewerRoleFor(actor: SupportActor): SupportThreadViewerRoleEnumDto =
        ownerUserId.viewerRoleFor(actor)

    private fun SupportThreadSummary.viewerRoleFor(actor: SupportActor): SupportThreadViewerRoleEnumDto =
        ownerUserId.viewerRoleFor(actor)

    private fun UUID.viewerRoleFor(actor: SupportActor): SupportThreadViewerRoleEnumDto =
        if (this == actor.userId) {
            SupportThreadViewerRoleEnumDto.REQUESTER
        } else {
            SupportThreadViewerRoleEnumDto.SUPPORT
        }
}
