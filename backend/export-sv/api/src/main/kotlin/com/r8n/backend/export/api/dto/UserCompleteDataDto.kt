package com.r8n.backend.export.api.dto

import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.messaging.api.dto.SupportThreadDto
import com.r8n.backend.opinions.api.access.dto.AccessRequestDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListDto
import com.r8n.backend.users.api.dto.UserStatusEnumDto
import com.r8n.backend.users.integration.api.dto.ConsentDto
import com.r8n.backend.users.integration.api.dto.PersonalIdentifiableInformationSectionDto
import java.time.Instant
import java.util.UUID

data class UserCompleteDataDto(
    val id: UUID,
    val status: UserStatusEnumDto,
    val statusTimestamp: Instant,
    val consents: PageResponseDto<ConsentDto>,
    val personalIdentifiableInformation: PersonalIdentifiableInformationSectionDto,
    val opinions: PageResponseDto<OpinionListDto>,
    val outgoingRequests: PageResponseDto<AccessRequestDto>,
    val incomingRequests: PageResponseDto<AccessRequestDto>,
    val messages: PageResponseDto<SupportThreadDto>,
)
