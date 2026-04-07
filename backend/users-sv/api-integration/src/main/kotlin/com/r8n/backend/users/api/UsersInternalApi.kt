package com.r8n.backend.users.integration.api

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.users.api.dto.UserDto
import com.r8n.backend.users.api.dto.UserSessionDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

interface UsersInternalApi {
    companion object {
        const val NAME_PATH = "/users/{id}/name"
    }

    @GetMapping(NAME_PATH)
    fun getUserName(
        @PathVariable id: UUID,
    ): String

    @GetMapping("/users/{id}")
    fun getUser(
        @PathVariable id: UUID,
    ): UserDto

    @GetMapping("/users/{userId}/sessions")
    fun getSessionsForUser(
        @PathVariable userId: UUID,
        page: PageRequestDto? = null,
    ): PageResponseDto<UserSessionDto>
}