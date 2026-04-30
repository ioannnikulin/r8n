package com.r8n.backend.messaging.controller

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.utils.toPageable
import com.r8n.backend.core.utils.toResponse
import com.r8n.backend.messaging.api.MessagingApi
import com.r8n.backend.messaging.api.dto.messaging.CreateSupportMessageRequestDto
import com.r8n.backend.messaging.api.dto.messaging.CreateSupportThreadRequestDto
import com.r8n.backend.messaging.api.dto.messaging.SupportMessageDto
import com.r8n.backend.messaging.api.dto.messaging.SupportThreadSummaryDto
import com.r8n.backend.messaging.facade.SupportMessagingFacade
import com.r8n.backend.messaging.persistence.SupportParticipantRoleEnumPersistence
import com.r8n.backend.messaging.service.SupportActor
import com.r8n.backend.security.Authority
import com.r8n.backend.security.Authority.IS_USER_OR_SUPPORT
import com.r8n.backend.security.CurrentUserIdentifier.getCurrentUserId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class MessagingController(
    private val supportMessagingFacade: SupportMessagingFacade,
) : MessagingApi {
    @PreAuthorize(IS_USER_OR_SUPPORT)
    override fun getSupportThreadSummaries(pageable: PageRequestDto) =
        supportMessagingFacade.getSupportThreadSummaries(getSupportActor(), pageable.toPageable()).toResponse()

    @PreAuthorize(IS_USER_OR_SUPPORT)
    override fun createSupportThread(request: CreateSupportThreadRequestDto): SupportThreadSummaryDto =
        supportMessagingFacade.createSupportThread(getSupportActor(), request)

    @PreAuthorize(IS_USER_OR_SUPPORT)
    override fun getSupportThreadMessages(
        threadId: UUID,
        pageable: PageRequestDto,
    ) = supportMessagingFacade.getSupportThreadMessages(getSupportActor(), threadId, pageable.toPageable()).toResponse()

    @PreAuthorize(IS_USER_OR_SUPPORT)
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
                .any { it.authority == Authority.SUPPORT }

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
}
