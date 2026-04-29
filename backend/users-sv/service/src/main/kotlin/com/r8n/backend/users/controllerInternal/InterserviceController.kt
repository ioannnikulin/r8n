package com.r8n.backend.users.controllerInternal

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.core.utils.toResponse
import com.r8n.backend.security.Authority
import com.r8n.backend.users.facade.UserFacade
import com.r8n.backend.users.integration.api.KeyValidationApi
import com.r8n.backend.users.integration.api.UsersInternalApi
import com.r8n.backend.users.integration.api.dto.UserDto
import com.r8n.backend.users.integration.api.dto.UserSessionDto
import com.r8n.backend.users.service.ApiKeyService
import com.r8n.backend.users.service.UserService
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class InterserviceController(
    private val userService: UserService,
    private val userFacade: UserFacade,
    private val apiKeyService: ApiKeyService,
) : UsersInternalApi,
    KeyValidationApi {
    @PreAuthorize(Authority.IS_USER_OR_SERVICE)
    override fun validateApiKey(key: String): UUID = apiKeyService.validateApiKey(key)

    @PreAuthorize(Authority.IS_USER_OR_SERVICE)
    override fun getUserName(id: UUID) = userService.getName(id)

    @PreAuthorize(Authority.IS_USER_OR_SERVICE)
    override fun isAnyModerator(id: UUID): Boolean = userService.isAnyModerator(id)

    @PreAuthorize(Authority.IS_USER_OR_SERVICE)
    override fun isHumanModerator(id: UUID): Boolean = userService.isHumanModerator(id)

    @PreAuthorize(Authority.IS_USER_OR_SERVICE)
    override fun isAiModerator(id: UUID): Boolean = userService.isAiModerator(id)

    @PreAuthorize(Authority.IS_USER_OR_SERVICE)
    override fun isAdmin(id: UUID): Boolean = userService.isAdmin(id)

    @PreAuthorize(Authority.IS_USER_OR_SERVICE)
    override fun getUser(id: UUID): UserDto = userFacade.getUser(id)

    @PreAuthorize(Authority.IS_USER_OR_SERVICE)
    override fun findUsersByNameSubstring(nameSubstring: String): List<UserDto> =
        userFacade.findUsersByNameSubstring(nameSubstring)

    @PreAuthorize(Authority.IS_USER_OR_SERVICE)
    override fun getSessionsForUser(
        id: UUID,
        page: PageRequestDto?,
    ): PageResponseDto<UserSessionDto> {
        val pageable = if (page != null) Pageable.ofSize(page.size).withPage(page.page) else Pageable.unpaged()
        val sessions = userFacade.getSessionsForUser(id, pageable)
        return sessions.toResponse()
    }
}
