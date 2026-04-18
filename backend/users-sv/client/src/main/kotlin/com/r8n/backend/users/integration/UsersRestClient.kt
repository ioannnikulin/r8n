package com.r8n.backend.users.integration

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.users.api.dto.UserDto
import com.r8n.backend.users.api.dto.UserSessionDto
import com.r8n.backend.users.integration.api.UsersInternalApi
import com.r8n.backend.users.integration.api.UsersInternalApi.Companion.ID_PATH
import com.r8n.backend.users.integration.api.UsersInternalApi.Companion.NAME_PATH
import com.r8n.backend.users.integration.api.UsersInternalApi.Companion.SESSIONS_PATH
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.client.RestClient
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

    override fun getUser(id: UUID): UserDto =
        restClient
            .get()
            .uri(ID_PATH, id)
            .retrieve()
            .body(UserDto::class.java)!!

    override fun getSessionsForUser(
        userId: UUID,
        page: PageRequestDto?,
    ): PageResponseDto<UserSessionDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(SESSIONS_PATH)
                    .queryParam("page", page?.page ?: 0)
                    .queryParam("size", page?.size ?: 20)
                    .build(userId)
            }.retrieve()
            .body(object : ParameterizedTypeReference<PageResponseDto<UserSessionDto>>() {})!!
}