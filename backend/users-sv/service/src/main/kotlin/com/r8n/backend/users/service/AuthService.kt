package com.r8n.backend.users.service

import com.r8n.backend.users.api.dto.LoginRequestDto
import com.r8n.backend.users.api.dto.RegisterRequestDto
import com.r8n.backend.users.domain.UserStatusEnum
import com.r8n.backend.users.provider.database.UserRepository
import com.r8n.backend.users.provider.database.UserRoleAssignmentRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val userRoleAssignmentRepository: UserRoleAssignmentRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val passwordEncoder: PasswordEncoder,
    private val tokenService: TokenService,
) {
    private companion object {
        val log: Logger = LoggerFactory.getLogger(AuthService::class.java)
        val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        const val EMAIL_MAX_LENGTH = 254
        const val NAME_MAX_LENGTH = 255
        const val PASSWORD_MIN_LENGTH = 12
        const val PASSWORD_MAX_LENGTH = 128
        const val REGISTRATION_CONSENT_SESSION_OS = "Unknown"
        val REGISTRATION_CONSENT_SESSION_DURATION: Duration = Duration.ofDays(1)
    }

    fun login(request: LoginRequestDto): AuthenticationTokens {
        val user =
            userRepository.findByNormalizedEmail(normalizeEmail(request.login))
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")

        if (user.passwordHash == null || !passwordEncoder.matches(request.password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        }

        val roles =
            (listOf("USER") + userRoleAssignmentRepository.findAllByUser(user.id).map { it.role.name }).distinct()
        val accessToken = tokenService.generateAccessToken(user.id, roles)
        val refreshToken = tokenService.generateRefreshToken(user.id)
        userRepository.updateLastSeenAt(user.id, Instant.now())

        return AuthenticationTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresInMilliseconds = tokenService.getAccessTokenExpirationMillis(),
        )
    }

    @Transactional
    fun register(
        request: RegisterRequestDto,
        auditContext: RegistrationAuditContext,
    ) {
        val normalizedEmail = validateAndNormalizeRegistrationEmail(request.email)
        val normalizedName = validateAndNormalizeRegistrationName(request.name)
        validateRegistrationPassword(request.password)
        validateRegistrationConsents(request)

        if (userRepository.findByNormalizedEmail(normalizedEmail) != null) {
            throw registrationConflict()
        }

        try {
            val now = Instant.now()
            val userId = UUID.randomUUID()
            val consentSessionId = UUID.randomUUID()

            jdbcTemplate.update(
                """
                INSERT INTO users.users (id, status, status_timestamp, password_hash)
                VALUES (?, ?, ?, ?)
                """.trimIndent(),
                userId,
                UserStatusEnum.ACTIVE.name,
                Timestamp.from(now),
                passwordEncoder.encode(request.password),
            )

            jdbcTemplate.update(
                """
                INSERT INTO users.pii (user_id, name, email, phone, about, location)
                VALUES (?, ?, ?, NULL, NULL, NULL)
                """.trimIndent(),
                userId,
                normalizedName,
                normalizedEmail,
            )

            jdbcTemplate.update(
                """
                INSERT INTO users.sessions (id, user_id, created, expires, ip, user_agent, os)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                consentSessionId,
                userId,
                Timestamp.from(now),
                Timestamp.from(now.plus(REGISTRATION_CONSENT_SESSION_DURATION)),
                auditContext.ip,
                auditContext.userAgent,
                REGISTRATION_CONSENT_SESSION_OS,
            )

            jdbcTemplate.update(
                """
                INSERT INTO users.consents (id, user_id, type, accepted, session)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent(),
                UUID.randomUUID(),
                userId,
                "PRIVACY_POLICY",
                Timestamp.from(now),
                consentSessionId,
            )
            jdbcTemplate.update(
                """
                INSERT INTO users.consents (id, user_id, type, accepted, session)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent(),
                UUID.randomUUID(),
                userId,
                "TERMS_OF_SERVICE",
                Timestamp.from(now),
                consentSessionId,
            )
        } catch (_: DataIntegrityViolationException) {
            throw registrationConflict()
        }
    }

    fun logout(refreshToken: String?) {
        if (refreshToken != null) {
            try {
                // When logging out, we want to revoke the whole token family
                // because this session is explicitly ended.
                val (userId, _) = tokenService.validateAndRotateRefreshToken(refreshToken)
                tokenService.revokeAllTokensForUser(userId)
            } catch (_: Exception) {
                log.warn("Failed to revoke refresh token")
            }
        }
    }

    fun logoutAll(userId: UUID) {
        tokenService.revokeAllTokensForUser(userId)
    }

    fun refresh(refreshToken: String?): AuthenticationTokens {
        val (userId, newRefreshToken) =
            tokenService.validateAndRotateRefreshToken(
                refreshToken ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh token"),
            )

        val roles =
            (listOf("USER") + userRoleAssignmentRepository.findAllByUser(userId).map { it.role.name }).distinct()
        val accessToken = tokenService.generateAccessToken(userId, roles)
        userRepository.updateLastSeenAt(userId, Instant.now())

        return AuthenticationTokens(
            accessToken = accessToken,
            refreshToken = newRefreshToken,
            expiresInMilliseconds = tokenService.getAccessTokenExpirationMillis(),
        )
    }

    private fun validateAndNormalizeRegistrationEmail(email: String): String {
        val normalizedEmail = normalizeEmail(email)

        if (normalizedEmail.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required")
        }
        if (normalizedEmail.length > EMAIL_MAX_LENGTH) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must be $EMAIL_MAX_LENGTH characters or fewer")
        }
        if (!EMAIL_PATTERN.matches(normalizedEmail)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is invalid")
        }

        return normalizedEmail
    }

    private fun validateAndNormalizeRegistrationName(name: String): String {
        val normalizedName = name.trim()

        if (normalizedName.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required")
        }
        if (normalizedName.length > NAME_MAX_LENGTH) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be $NAME_MAX_LENGTH characters or fewer")
        }

        return normalizedName
    }

    private fun validateRegistrationPassword(password: String) {
        if (password.trim().isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required")
        }
        if (password.length < PASSWORD_MIN_LENGTH) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Password must be at least $PASSWORD_MIN_LENGTH characters",
            )
        }
        if (password.length > PASSWORD_MAX_LENGTH) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Password must be $PASSWORD_MAX_LENGTH characters or fewer",
            )
        }
    }

    private fun validateRegistrationConsents(request: RegisterRequestDto) {
        if (!request.privacyPolicyAccepted || !request.termsOfServiceAccepted) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Privacy Policy and Terms of Service must be accepted",
            )
        }
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.ROOT)

    private fun registrationConflict() =
        ResponseStatusException(HttpStatus.CONFLICT, "Registration could not be completed")
}
