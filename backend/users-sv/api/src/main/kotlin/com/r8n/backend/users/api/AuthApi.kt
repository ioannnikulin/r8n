package com.r8n.backend.users.api

import com.r8n.backend.users.api.dto.AuthenticationTokenDto
import com.r8n.backend.users.api.dto.LoginRequestDto
import com.r8n.backend.users.api.dto.RegisterRequestDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus

@Tag(name = "Authentication", description = "Registration, login, logout, refresh-token, and CSRF endpoints.")
interface AuthApi {
    companion object {
        const val REFRESH_TOKEN_COOKIE_NAME = "refreshToken"
        private const val ROOT_PATH = "/api/auth"
        const val CSRF_PATH = "$ROOT_PATH/csrf"
        const val LOGIN_PATH = "$ROOT_PATH/login"
        const val REGISTER_PATH = "$ROOT_PATH/register"
        const val LOGOUT_PATH = "$ROOT_PATH/logout"
        const val REFRESH_PATH = "$ROOT_PATH/refresh"
    }

    @GetMapping(CSRF_PATH)
    @Operation(
        summary = "Create CSRF cookie",
        description = "Issues a non-HTTP-only XSRF-TOKEN cookie for browser clients before state-changing requests.",
    )
    fun csrf()

    @PostMapping(LOGIN_PATH)
    @Operation(
        summary = "Log in",
        description = "Authenticates a user, returns an access token, and sets the refresh token cookie.",
    )
    fun login(
        @RequestBody request: LoginRequestDto,
    ): AuthenticationTokenDto

    @PostMapping(REGISTER_PATH)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Register account",
        description = "Creates a new user account and records minimal registration audit context.",
    )
    fun register(
        @RequestBody request: RegisterRequestDto,
    )

    @PostMapping(LOGOUT_PATH)
    @Operation(
        summary = "Log out",
        description =
            "Ends the user's session. " +
                "Invalidates the refresh token when present and clears the refresh token cookie.",
    )
    fun logout(
        @Parameter(description = "Refresh token cookie issued by login or refresh.")
        @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) refreshToken: String?,
    )

    @PostMapping(REFRESH_PATH)
    @Operation(
        summary = "Refresh access token",
        description = "Rotates the refresh token cookie and returns a new short-lived access token.",
    )
    fun refresh(
        @Parameter(description = "Refresh token cookie issued by login or a previous refresh.")
        @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) refreshToken: String?,
    ): AuthenticationTokenDto
}
