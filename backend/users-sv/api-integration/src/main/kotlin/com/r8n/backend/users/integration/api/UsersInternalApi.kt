package com.r8n.backend.users.integration.api

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.users.integration.api.dto.UserDto
import com.r8n.backend.users.integration.api.dto.UserSessionDto
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

interface UsersInternalApi {
    companion object {
        const val ID_PATH = "/api/internal/users/{id}"
        const val NAME_PATH = "$ID_PATH/name"
        const val SESSIONS_PATH = "$ID_PATH/sessions"
        const val IS_ANY_MODERATOR_PATH = "$ID_PATH/is-any-moderator"
        const val IS_HUMAN_MODERATOR_PATH = "$ID_PATH/is-human-moderator"
        const val IS_AI_MODERATOR_PATH = "$ID_PATH/is-ai-moderator"
        const val IS_ADMIN_PATH = "$ID_PATH/is-admin"
        const val SEARCH_PATH = "/api/internal/users/search"
    }

    @GetMapping(SEARCH_PATH)
    fun findUsersByNameSubstring(
        @RequestParam nameSubstring: String,
    ): List<UserDto>

    @GetMapping(NAME_PATH)
    fun getUserName(
        @PathVariable id: UUID,
    ): String

    @GetMapping(ID_PATH)
    fun getUser(
        @PathVariable id: UUID,
    ): UserDto

    @GetMapping(SESSIONS_PATH)
    fun getSessionsForUser(
        @PathVariable id: UUID,
        @Valid
        page: PageRequestDto? = null,
    ): PageResponseDto<UserSessionDto>

    @GetMapping(IS_ANY_MODERATOR_PATH)
    fun isAnyModerator(
        @PathVariable id: UUID,
    ): Boolean

    @GetMapping(IS_HUMAN_MODERATOR_PATH)
    fun isHumanModerator(
        @PathVariable id: UUID,
    ): Boolean

    @GetMapping(IS_AI_MODERATOR_PATH)
    fun isAiModerator(
        @PathVariable id: UUID,
    ): Boolean

    @GetMapping(IS_ADMIN_PATH)
    fun isAdmin(
        @PathVariable id: UUID,
    ): Boolean
}
