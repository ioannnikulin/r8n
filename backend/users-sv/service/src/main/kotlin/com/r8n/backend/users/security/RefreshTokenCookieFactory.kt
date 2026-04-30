package com.r8n.backend.users.security

import com.r8n.backend.users.api.AuthApi.Companion.REFRESH_TOKEN_COOKIE_NAME
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RefreshTokenCookieFactory(
    @Value("\${r8n.security.refresh-cookie.secure}") private val secure: Boolean,
    @Value("\${r8n.security.refresh-cookie.same-site}") private val sameSite: String,
    @Value("\${r8n.security.refresh-cookie.path}") private val path: String,
    @Value("\${r8n.security.refresh-cookie.max-age}") private val maxAge: Duration,
) {
    fun create(refreshToken: String): ResponseCookie =
        baseCookie(refreshToken)
            .maxAge(maxAge)
            .build()

    fun clear(): ResponseCookie =
        baseCookie("")
            .maxAge(Duration.ZERO)
            .build()

    private fun baseCookie(value: String) =
        ResponseCookie
            .from(REFRESH_TOKEN_COOKIE_NAME, value)
            .httpOnly(true)
            .secure(secure)
            .path(path)
            .sameSite(sameSite)
}
