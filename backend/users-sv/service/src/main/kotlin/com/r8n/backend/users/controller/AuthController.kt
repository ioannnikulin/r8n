package com.r8n.backend.users.controller

import com.r8n.backend.users.api.AuthApi
import com.r8n.backend.users.api.dto.AuthenticationTokenDto
import com.r8n.backend.users.api.dto.LoginRequestDto
import com.r8n.backend.users.api.dto.RegisterRequestDto
import com.r8n.backend.users.security.RefreshTokenCookieFactory
import com.r8n.backend.users.service.AuthService
import com.r8n.backend.users.service.RegistrationAuditContext
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.UUID

@RestController
class AuthController(
    private val authService: AuthService,
    private val refreshTokenCookieFactory: RefreshTokenCookieFactory,
) : AuthApi {
    override fun csrf() {
        currentResponse().addHeader(
            HttpHeaders.SET_COOKIE,
            createXsrfCookie().toString(),
        )
    }

    override fun login(request: LoginRequestDto): AuthenticationTokenDto {
        val tokens = authService.login(request)
        addRefreshTokenCookie(tokens.refreshToken)
        return AuthenticationTokenDto(
            accessToken = tokens.accessToken,
            expiresInMilliseconds = tokens.expiresInMilliseconds,
        )
    }

    override fun register(request: RegisterRequestDto) {
        authService.register(
            request = request,
            auditContext =
                RegistrationAuditContext(
                    ip = currentClientIp(),
                    userAgent = currentRequest().getHeader(HttpHeaders.USER_AGENT) ?: "Unknown",
                ),
        )
    }

    override fun logout(refreshToken: String?) {
        authService.logout(refreshToken)
        clearRefreshTokenCookie()
    }

    override fun refresh(refreshToken: String?): AuthenticationTokenDto {
        val tokens = authService.refresh(refreshToken)
        addRefreshTokenCookie(tokens.refreshToken)
        return AuthenticationTokenDto(
            accessToken = tokens.accessToken,
            expiresInMilliseconds = tokens.expiresInMilliseconds,
        )
    }

    private fun addRefreshTokenCookie(refreshToken: String) {
        currentResponse().addHeader(
            HttpHeaders.SET_COOKIE,
            refreshTokenCookieFactory.create(refreshToken).toString(),
        )
    }

    private fun clearRefreshTokenCookie() {
        currentResponse().addHeader(
            HttpHeaders.SET_COOKIE,
            refreshTokenCookieFactory.clear().toString(),
        )
    }

    private fun createXsrfCookie() =
        ResponseCookie
            .from("XSRF-TOKEN", UUID.randomUUID().toString())
            .httpOnly(false)
            .secure(currentRequest().isSecure)
            .path("/")
            .build()

    private fun currentRequest() = currentRequestAttributes().request

    private fun currentClientIp(): String {
        val forwardedFor = currentRequest().getHeader("X-Forwarded-For")
        val candidate = forwardedFor?.substringBefore(",")?.trim()?.takeIf { it.isNotEmpty() }
        return candidate?.takeIf { it.length <= 45 } ?: currentRequest().remoteAddr
    }

    private fun currentResponse() =
        currentRequestAttributes().response
            ?: throw IllegalStateException("No current HTTP response")

    private fun currentRequestAttributes() = RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
}
