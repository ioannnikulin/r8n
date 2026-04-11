package com.r8n.backend.users.controller

import com.r8n.backend.security.Authority.IS_USER
import com.r8n.backend.users.api.UsersApi
import com.r8n.backend.users.api.dto.UserCompleteDataDto
import com.r8n.backend.users.facade.UserFacade
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class UserController(
    private val userFacade: UserFacade,
) : UsersApi {
    @PreAuthorize(IS_USER)
    override fun exportAll(): UserCompleteDataDto {
        val auth = SecurityContextHolder.getContext().authentication ?: throw IllegalStateException("Not authenticated")
        val userId = UUID.fromString(auth.name)
        return userFacade.getUserCompleteDataDto(userId)
    }
}