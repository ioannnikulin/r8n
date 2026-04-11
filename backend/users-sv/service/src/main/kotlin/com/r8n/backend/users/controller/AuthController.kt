package com.r8n.backend.users.controller

import com.r8n.backend.users.api.AuthApi
import com.r8n.backend.users.api.dto.AuthenticationTokenDto
import com.r8n.backend.users.api.dto.LoginRequestDto
import com.r8n.backend.users.service.AuthService
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val authService: AuthService,
) : AuthApi {
    override fun login(request: LoginRequestDto): AuthenticationTokenDto = authService.login(request)

    override fun logout() {
        authService.logout()
    }

    override fun refresh(refreshToken: String): AuthenticationTokenDto = authService.refresh(refreshToken)
}