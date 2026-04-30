package com.r8n.backend.users

import com.r8n.backend.opinions.api.access.IncomingAccessRequestApi
import com.r8n.backend.opinions.api.access.OutgoingAccessRequestApi
import com.r8n.backend.opinions.integration.api.OpinionListsInternalApi
import com.r8n.backend.users.persistence.RefreshTokenPersistence
import com.r8n.backend.users.provider.database.RefreshTokenRepository
import com.r8n.backend.users.service.TokenService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestObjectMapperConfiguration::class)
@MockitoBean(
    types = [
        IncomingAccessRequestApi::class,
        OutgoingAccessRequestApi::class,
        OpinionListsInternalApi::class,
    ],
)
class TokenCleanupIntegrationTest {
    private companion object {
        @Suppress("unused") // used to store test database container
        @Container
        @ServiceConnection
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer(DockerImageName.parse("postgres:15"))
                .withDatabaseName("users")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("db/init-schema.sql")
    }

    @Autowired
    lateinit var tokenService: TokenService

    @Autowired
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Test
    fun `cleanupExpiredTokens removes only expired tokens`() {
        // Given
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val now = Instant.now()

        val expiredToken =
            RefreshTokenPersistence(
                tokenId = UUID.randomUUID(),
                userId = userId,
                issuedAt = now.minus(2, ChronoUnit.DAYS),
                expiresAt = now.minus(1, ChronoUnit.DAYS),
                revoked = false,
                used = false,
            )

        val validToken =
            RefreshTokenPersistence(
                tokenId = UUID.randomUUID(),
                userId = userId,
                issuedAt = now.minus(1, ChronoUnit.DAYS),
                expiresAt = now.plus(1, ChronoUnit.DAYS),
                revoked = false,
                used = false,
            )

        refreshTokenRepository.saveAll(listOf(expiredToken, validToken))

        val countBefore = refreshTokenRepository.count()

        // When
        tokenService.cleanupExpiredTokens()

        // Then
        val remainingTokens = refreshTokenRepository.findAll()
        assertEquals(countBefore - 1, remainingTokens.size.toLong())
        assertEquals(validToken.tokenId, remainingTokens[0].tokenId)
    }
}
