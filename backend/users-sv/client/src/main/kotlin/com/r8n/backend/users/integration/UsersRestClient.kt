package com.r8n.backend.users.integration

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.users.integration.api.UsersInternalApi
import com.r8n.backend.users.integration.api.UsersInternalApi.Companion.ID_PATH
import com.r8n.backend.users.integration.api.UsersInternalApi.Companion.IS_ADMIN_PATH
import com.r8n.backend.users.integration.api.UsersInternalApi.Companion.IS_AI_MODERATOR_PATH
import com.r8n.backend.users.integration.api.UsersInternalApi.Companion.IS_ANY_MODERATOR_PATH
import com.r8n.backend.users.integration.api.UsersInternalApi.Companion.IS_HUMAN_MODERATOR_PATH
import com.r8n.backend.users.integration.api.UsersInternalApi.Companion.NAME_PATH
import com.r8n.backend.users.integration.api.UsersInternalApi.Companion.SEARCH_PATH
import com.r8n.backend.users.integration.api.UsersInternalApi.Companion.SESSIONS_PATH
import com.r8n.backend.users.integration.api.dto.UserDto
import com.r8n.backend.users.integration.api.dto.UserSessionDto
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.util.UUID

class UsersRestClient(
    private val restClient: RestClient,
) : UsersInternalApi {
    override fun getUserName(id: UUID): String =
        restClient
            .get()
            .uri(NAME_PATH, id)
            .retrieve()
            .body(String::class.java)!!

    override fun findUsersByNameSubstring(nameSubstring: String): List<UserDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(SEARCH_PATH)
                    .queryParam("nameSubstring", nameSubstring)
                    .build()
            }.retrieve()
            .body(object : ParameterizedTypeReference<List<UserDto>>() {})!!

    override fun getUser(id: UUID): UserDto =
        restClient
            .get()
            .uri(ID_PATH, id)
            .retrieve()
            .body(UserDto::class.java)!!

    override fun getSessionsForUser(
        id: UUID,
        page: PageRequestDto?,
    ): PageResponseDto<UserSessionDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(SESSIONS_PATH)
                    .queryParam("page", page?.page ?: 0)
                    .queryParam("size", page?.size ?: 20)
                    .build(id)
            }.retrieve()
            .body(object : ParameterizedTypeReference<PageResponseDto<UserSessionDto>>() {})!!

    override fun isAnyModerator(id: UUID): Boolean =
        restClient
            .get()
            .uri(IS_ANY_MODERATOR_PATH, id)
            .retrieve()
            .body<Boolean>()!!

    override fun isHumanModerator(id: UUID): Boolean =
        restClient
            .get()
            .uri(IS_HUMAN_MODERATOR_PATH, id)
            .retrieve()
            .body<Boolean>()!!

    override fun isAiModerator(id: UUID): Boolean =
        restClient
            .get()
            .uri(IS_AI_MODERATOR_PATH, id)
            .retrieve()
            .body<Boolean>()!!

    override fun isAdmin(id: UUID): Boolean =
        restClient
            .get()
            .uri(IS_ADMIN_PATH, id)
            .retrieve()
            .body<Boolean>()!!
}
