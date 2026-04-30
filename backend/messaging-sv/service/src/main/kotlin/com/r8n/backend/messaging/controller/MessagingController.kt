package com.r8n.backend.messaging.controller

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.utils.toPageable
import com.r8n.backend.core.utils.toResponse
import com.r8n.backend.messaging.api.MessagingApi
import com.r8n.backend.messaging.api.dto.messaging.CreateDirectConversationRequestDto
import com.r8n.backend.messaging.api.dto.messaging.CreateDirectMessageRequestDto
import com.r8n.backend.messaging.api.dto.messaging.CreateSupportMessageRequestDto
import com.r8n.backend.messaging.api.dto.messaging.CreateSupportThreadRequestDto
import com.r8n.backend.messaging.api.dto.messaging.DirectMessageDto
import com.r8n.backend.messaging.api.dto.messaging.SupportMessageDto
import com.r8n.backend.messaging.api.dto.messaging.SupportThreadSummaryDto
import com.r8n.backend.messaging.api.dto.messaging.UnreadMessagesCountDto
import com.r8n.backend.messaging.facade.DirectMessagingFacade
import com.r8n.backend.messaging.facade.SupportMessagingFacade
import com.r8n.backend.messaging.persistence.MessageAuthorRoleEnumPersistence
import com.r8n.backend.messaging.persistence.SupportParticipantRoleEnumPersistence
import com.r8n.backend.messaging.service.DirectActor
import com.r8n.backend.messaging.service.SupportActor
import com.r8n.backend.security.Authority
import com.r8n.backend.security.Authority.IS_EXPLICIT_USER_OR_MODERATOR_OR_SUPPORT_OR_ADMIN
import com.r8n.backend.security.CurrentUserIdentifier.getCurrentUserId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class MessagingController(
    private val directMessagingFacade: DirectMessagingFacade,
    private val supportMessagingFacade: SupportMessagingFacade,
) : MessagingApi {
    @PreAuthorize(IS_EXPLICIT_USER_OR_MODERATOR_OR_SUPPORT_OR_ADMIN)
    override fun getUnreadMessagesCount(): UnreadMessagesCountDto {
        val directActor = getDirectActor()
        return UnreadMessagesCountDto(
            count =
                directMessagingFacade.countUnreadMessages(directActor) +
                    supportMessagingFacade.countUnreadMessages(getSupportActor()),
        )
    }

    @PreAuthorize(IS_EXPLICIT_USER_OR_MODERATOR_OR_SUPPORT_OR_ADMIN)
    override fun getDirectConversationSummaries(pageable: PageRequestDto) =
        directMessagingFacade.getDirectConversationSummaries(getDirectActor(), pageable.toPageable()).toResponse()

    @PreAuthorize(IS_EXPLICIT_USER_OR_MODERATOR_OR_SUPPORT_OR_ADMIN)
    override fun createDirectConversation(request: CreateDirectConversationRequestDto) =
        directMessagingFacade.createDirectConversation(getDirectActor(), request)

    @PreAuthorize(IS_EXPLICIT_USER_OR_MODERATOR_OR_SUPPORT_OR_ADMIN)
    override fun getDirectConversationMessages(
        conversationId: UUID,
        pageable: PageRequestDto,
    ) = directMessagingFacade
        .getDirectConversationMessages(
            getDirectActor(),
            conversationId,
            pageable.toPageable(),
        ).toResponse()

    @PreAuthorize(IS_EXPLICIT_USER_OR_MODERATOR_OR_SUPPORT_OR_ADMIN)
    override fun addDirectConversationMessage(
        conversationId: UUID,
        request: CreateDirectMessageRequestDto,
    ): DirectMessageDto = directMessagingFacade.addDirectConversationMessage(getDirectActor(), conversationId, request)

    @PreAuthorize(IS_EXPLICIT_USER_OR_MODERATOR_OR_SUPPORT_OR_ADMIN)
    override fun markDirectConversationAsRead(conversationId: UUID) =
        directMessagingFacade.markConversationAsRead(getDirectActor(), conversationId)

    @PreAuthorize(IS_EXPLICIT_USER_OR_MODERATOR_OR_SUPPORT_OR_ADMIN)
    override fun getSupportThreadSummaries(pageable: PageRequestDto) =
        supportMessagingFacade.getSupportThreadSummaries(getSupportActor(), pageable.toPageable()).toResponse()

    @PreAuthorize(IS_EXPLICIT_USER_OR_MODERATOR_OR_SUPPORT_OR_ADMIN)
    override fun createSupportThread(request: CreateSupportThreadRequestDto): SupportThreadSummaryDto =
        supportMessagingFacade.createSupportThread(getSupportActor(), request)

    @PreAuthorize(IS_EXPLICIT_USER_OR_MODERATOR_OR_SUPPORT_OR_ADMIN)
    override fun getSupportThreadMessages(
        threadId: UUID,
        pageable: PageRequestDto,
    ) = supportMessagingFacade.getSupportThreadMessages(getSupportActor(), threadId, pageable.toPageable()).toResponse()

    @PreAuthorize(IS_EXPLICIT_USER_OR_MODERATOR_OR_SUPPORT_OR_ADMIN)
    override fun deleteSupportThread(threadId: UUID) =
        supportMessagingFacade.deleteSupportThread(getSupportActor(), threadId)

    @PreAuthorize(IS_EXPLICIT_USER_OR_MODERATOR_OR_SUPPORT_OR_ADMIN)
    override fun addSupportThreadMessage(
        threadId: UUID,
        request: CreateSupportMessageRequestDto,
    ): SupportMessageDto = supportMessagingFacade.addSupportThreadMessage(getSupportActor(), threadId, request)

    private fun getSupportActor(): SupportActor {
        val isSupport =
            SecurityContextHolder
                .getContext()
                .authentication
                ?.authorities
                .orEmpty()
                .any { it.authority == Authority.SUPPORT || it.authority == Authority.ADMIN }

        return SupportActor(
            userId = getCurrentUserId(),
            role =
                if (isSupport) {
                    SupportParticipantRoleEnumPersistence.SUPPORT
                } else {
                    SupportParticipantRoleEnumPersistence.USER
                },
        )
    }

    private fun getDirectActor(): DirectActor {
        val authorities =
            SecurityContextHolder
                .getContext()
                .authentication
                ?.authorities
                .orEmpty()
                .map { it.authority }
                .toSet()

        return DirectActor(
            userId = getCurrentUserId(),
            role =
                when {
                    Authority.ADMIN in authorities -> MessageAuthorRoleEnumPersistence.ADMIN
                    Authority.SUPPORT in authorities -> MessageAuthorRoleEnumPersistence.SUPPORT
                    Authority.MODERATOR in authorities -> MessageAuthorRoleEnumPersistence.MODERATOR
                    else -> MessageAuthorRoleEnumPersistence.USER
                },
        )
    }
}
