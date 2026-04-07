package com.r8n.backend.users.controller

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.core.utils.toResponse
import com.r8n.backend.security.Authority
import com.r8n.backend.users.api.dto.ConsentDto
import com.r8n.backend.users.api.dto.UserDto
import com.r8n.backend.users.api.dto.UserSessionDto
import com.r8n.backend.users.api.dto.UserStatusEnumDto
import com.r8n.backend.users.domain.User
import com.r8n.backend.users.integration.api.UsersInternalApi
import com.r8n.backend.users.service.ConsentService
import com.r8n.backend.users.service.UserService
import com.r8n.backend.users.service.UserSessionService
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class InterserviceController(
    private val userService: UserService,
    private val sessionService: UserSessionService,
    private val consentService: ConsentService,
) : UsersInternalApi {
    @PreAuthorize(Authority.IS_USER_OR_SERVICE)
    override fun getUserName(id: UUID) = userService.getName(id)

    override fun getUser(id: UUID): UserDto {
        val user = userService.getUser(id)
        val consents = consentService.getConsentsForUser(id)

        return UserDto(
            id = user.id,
            name = user.name,
            email = user.email,
            status = user.status.toDto(),
            statusTimestamp = user.statusTimestamp,
            consents = consents.map { it.toDto() }
        )
    }

    override fun getSessionsForUser(userId: UUID, page: PageRequestDto?): PageResponseDto<UserSessionDto> {
        val pageable = if (page != null) Pageable.ofSize(page.size).withPage(page.page) else Pageable.unpaged()
        val sessions = sessionService.getSessionsForUser(userId, pageable)
        return sessions.map { it.toDto() }.toResponse()
    }

    private fun com.r8n.backend.users.domain.UserStatusEnum.toDto() = UserStatusEnumDto.valueOf(this.name)

    private fun com.r8n.backend.users.domain.Consent.toDto() = ConsentDto(
        type = type,
        accepted = accepted,
        session = session.toDto()
    )

    private fun com.r8n.backend.users.domain.UserSession.toDto() = UserSessionDto(
        id = id,
        created = created,
        expires = expires,
        ip = ip,
        userAgent = userAgent
    )
}