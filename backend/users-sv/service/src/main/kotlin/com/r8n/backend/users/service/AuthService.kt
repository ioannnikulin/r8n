package com.r8n.backend.users.service

import com.r8n.backend.users.api.dto.AuthenticationTokenDto
import com.r8n.backend.users.api.dto.LoginRequestDto
import com.r8n.backend.users.provider.database.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenService: TokenService,
) {
    fun login(request: LoginRequestDto): AuthenticationTokenDto {
        val user = userRepository.findByEmail(request.login)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")

        if (user.passwordHash == null || !passwordEncoder.matches(request.password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        }

        val accessToken = tokenService.generateAccessToken(user.id, listOf("USER"))
        val refreshToken = tokenService.generateRefreshToken(user.id)

        return AuthenticationTokenDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresInMilliseconds = tokenService.getAccessTokenExpirationMillis()
        )
    }

    fun logout() {
        // In a stateless JWT setup, logout on server side is often a no-op unless we use a blacklist
        // For now, we just let the client discard the token.
    }
}
