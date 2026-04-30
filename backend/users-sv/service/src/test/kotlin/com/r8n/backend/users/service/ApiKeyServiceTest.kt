package com.r8n.backend.users.service

import com.r8n.backend.users.persistence.ApiKeyPersistence
import com.r8n.backend.users.provider.database.ApiKeyRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ApiKeyServiceTest {
    @Mock
    private lateinit var apiKeyRepository: ApiKeyRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var apiKeyService: ApiKeyService

    @BeforeEach
    fun setUp() {
        apiKeyService = ApiKeyService(apiKeyRepository, passwordEncoder)
    }

    @Test
    fun `createApiKey should generate a valid raw key with identifier`() {
        val userId = UUID.randomUUID()
        val name = "Test Key"

        whenever(passwordEncoder.encode(anyString())).thenReturn("hashed_key")

        val rawKeyWithPrefix = apiKeyService.createApiKey(userId, name)

        assertNotNull(rawKeyWithPrefix)
        assertTrue(rawKeyWithPrefix.startsWith("r8n_"))

        verify(apiKeyRepository).save(any(ApiKeyPersistence::class.java))
    }

    @Test
    fun `validateApiKey should return userId for valid key`() {
        val userId = UUID.randomUUID()
        val identifier = "ident1234567"
        val rawKey = "rawkeycontent"
        val rawKeyWithPrefix = "r8n_${identifier}_$rawKey"
        val hashedKey = "hashed_key"

        val apiKey =
            ApiKeyPersistence(
                userId = userId,
                keyIdentifier = identifier,
                keyHash = hashedKey,
                name = "Test Key",
                createdAt = Instant.now(),
            )

        whenever(apiKeyRepository.findByKeyIdentifier(identifier)).thenReturn(apiKey)
        whenever(passwordEncoder.matches(rawKey, hashedKey)).thenReturn(true)

        val validatedUserId = apiKeyService.validateApiKey(rawKeyWithPrefix)

        assertEquals(userId, validatedUserId)
        assertNotNull(apiKey.lastUsedAt)
        verify(apiKeyRepository).save(apiKey)
    }

    @Test
    fun `validateApiKey should throw exception for invalid format`() {
        assertThrows(ResponseStatusException::class.java) {
            apiKeyService.validateApiKey("invalid_key")
        }
    }

    @Test
    fun `validateApiKey should throw exception for non-existent identifier`() {
        whenever(apiKeyRepository.findByKeyIdentifier(anyString())).thenReturn(null)

        assertThrows(ResponseStatusException::class.java) {
            apiKeyService.validateApiKey("r8n_notfound_rawkey")
        }
    }

    @Test
    fun `validateApiKey should throw exception for incorrect raw key`() {
        val identifier = "id123"
        val apiKey =
            ApiKeyPersistence(
                userId = UUID.randomUUID(),
                keyIdentifier = identifier,
                keyHash = "hashed",
                name = "Test Key",
                createdAt = Instant.now(),
            )

        whenever(apiKeyRepository.findByKeyIdentifier(identifier)).thenReturn(apiKey)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(false)

        assertThrows(ResponseStatusException::class.java) {
            apiKeyService.validateApiKey("r8n_id123_wrongraw")
        }
    }

    @Test
    fun `validateApiKey should throw exception for revoked key`() {
        val identifier = "id123"
        val apiKey =
            ApiKeyPersistence(
                userId = UUID.randomUUID(),
                keyIdentifier = identifier,
                keyHash = "hashed",
                name = "Test Key",
                createdAt = Instant.now(),
                revoked = true,
            )

        whenever(apiKeyRepository.findByKeyIdentifier(identifier)).thenReturn(apiKey)

        assertThrows(ResponseStatusException::class.java) {
            apiKeyService.validateApiKey("r8n_id123_raw")
        }
    }

    @Test
    fun `validateApiKey should throw exception for expired key`() {
        val identifier = "id123"
        val apiKey =
            ApiKeyPersistence(
                userId = UUID.randomUUID(),
                keyIdentifier = identifier,
                keyHash = "hashed",
                name = "Test Key",
                createdAt = Instant.now(),
                expiresAt = Instant.now().minusSeconds(10),
            )

        whenever(apiKeyRepository.findByKeyIdentifier(identifier)).thenReturn(apiKey)

        assertThrows(ResponseStatusException::class.java) {
            apiKeyService.validateApiKey("r8n_id123_raw")
        }
    }
}
