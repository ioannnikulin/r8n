package com.r8n.backend.users.api

import com.r8n.backend.users.api.dto.AuthenticationTokenDto
import com.r8n.backend.users.api.dto.LoginRequestDto
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader

interface AuthApi {
    @PostMapping("/auth/login")
    fun login(
        @RequestBody request: LoginRequestDto,
    ): AuthenticationTokenDto

    @PostMapping("/auth/logout")
    fun logout()

    @PostMapping("/auth/refresh")
    fun refresh(
        @RequestHeader("X-Refresh-Token") refreshToken: String,
    ): AuthenticationTokenDto
}