package com.r8n.backend.users.service

import com.r8n.backend.users.persistence.ApiKeyPersistence
import com.r8n.backend.users.provider.database.ApiKeyRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Service
class ApiKeyService(
    private val apiKeyRepository: ApiKeyRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun createApiKey(
        userId: UUID,
        name: String,
        expiresAt: Instant? = null,
    ): String {
        val rawKey = generateSecureKey()

        // Use URL-safe Base64 for the identifier to avoid issues with special characters.
        // We also remove underscores from the identifier to use it as a clean part of the key.
        val identifier =
            UUID
                .randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)

        // We hash the full key with BCrypt for security.
        val hashedKey = passwordEncoder.encode(rawKey)

        val apiKey =
            ApiKeyPersistence(
                userId = userId,
                keyIdentifier = identifier,
                keyHash = hashedKey!!,
                name = name,
                createdAt = Instant.now(),
                expiresAt = expiresAt,
            )
        apiKeyRepository.save(apiKey)

        // Return prefix + identifier + raw key
        return "r8n_${identifier}_$rawKey"
    }

    @Transactional
    fun validateApiKey(rawKeyWithPrefix: String): UUID {
        val prefix = "r8n_"
        if (!rawKeyWithPrefix.startsWith(prefix)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key format")
        }

        val content = rawKeyWithPrefix.substring(prefix.length)
        val parts = content.split("_")
        if (parts.size != 2) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key format")
        }

        val identifier = parts[0]
        val rawKey = parts[1]

        val apiKey =
            apiKeyRepository.findByKeyIdentifier(identifier)
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key")

        if (apiKey.revoked || (apiKey.expiresAt != null && apiKey.expiresAt.isBefore(Instant.now()))) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "API key revoked or expired")
        }

        if (!passwordEncoder.matches(rawKey, apiKey.keyHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key")
        }

        apiKey.lastUsedAt = Instant.now()
        apiKeyRepository.save(apiKey)

        return apiKey.userId
    }

    private fun generateSecureKey(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getEncoder().withoutPadding().encodeToString(bytes)
    }

    @Transactional
    fun revokeApiKey(
        userId: UUID,
        keyId: UUID,
    ) {
        val apiKey =
            apiKeyRepository
                .findById(keyId)
                .filter { it.userId == userId }
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found") }

        apiKey.revoked = true
        apiKeyRepository.save(apiKey)
    }
}
