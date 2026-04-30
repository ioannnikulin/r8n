package com.r8n.backend.users.service

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.r8n.backend.security.SecurityCommonConfiguration.Companion.decodePublicKey
import com.r8n.backend.security.ServiceTokenService.Companion.decodePrivateKey
import com.r8n.backend.users.persistence.RefreshTokenPersistence
import com.r8n.backend.users.provider.database.RefreshTokenRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

@Service
class TokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    @Value("\${r8n.security.jwt.private-key:}") private val privateKeyPem: String,
    @Value("\${r8n.security.jwt.public-key:}") private val publicKeyPem: String,
    @Value("\${r8n.security.jwt.issuer:r8n}") private val issuer: String,
    @Value("\${r8n.security.jwt.access-token-expiration:1h}") private val accessTokenExpiration: Duration,
    @Value("\${r8n.security.jwt.refresh-token-expiration:30d}") private val refreshTokenExpiration: Duration,
) {
    private val signer: JWSSigner by lazy {
        if (privateKeyPem.isBlank()) {
            throw IllegalStateException("Private key for JWT signing is not provided")
        }
        RSASSASigner(decodePrivateKey(privateKeyPem))
    }

    fun generateAccessToken(
        userId: UUID,
        roles: List<String>,
    ): String {
        val now = Instant.now()
        val claimsSet =
            JWTClaimsSet
                .Builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim("roles", roles)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(accessTokenExpiration)))
                .build()

        val signedJWT = SignedJWT(JWSHeader(JWSAlgorithm.RS256), claimsSet)
        signedJWT.sign(signer)
        return signedJWT.serialize()
    }

    @Transactional
    fun generateRefreshToken(
        userId: UUID,
        parentId: UUID? = null,
    ): String {
        val now = Instant.now()
        val tokenId = UUID.randomUUID()
        val expiresAt = now.plus(refreshTokenExpiration)
        val claimsSet =
            JWTClaimsSet
                .Builder()
                .issuer(issuer)
                .subject(userId.toString())
                .jwtID(tokenId.toString())
                .claim("refresh", true)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .build()

        val signedJWT = SignedJWT(JWSHeader(JWSAlgorithm.RS256), claimsSet)
        signedJWT.sign(signer)

        refreshTokenRepository.save(
            RefreshTokenPersistence(
                tokenId = tokenId,
                userId = userId,
                parentId = parentId,
                issuedAt = now,
                expiresAt = expiresAt,
            ),
        )

        return signedJWT.serialize()
    }

    @Transactional
    fun validateAndRotateRefreshToken(refreshToken: String): Pair<UUID, String> {
        try {
            val signedJWT = SignedJWT.parse(refreshToken)
            val pubKey = decodePublicKey(publicKeyPem)
            val verifier = RSASSAVerifier(pubKey)

            if (!signedJWT.verify(verifier)) {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token")
            }

            val claims = signedJWT.jwtClaimsSet
            if (claims.issuer != issuer) {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token issuer")
            }

            if (Date().after(claims.expirationTime)) {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired")
            }

            if (claims.getClaim("refresh") != true) {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not a refresh token")
            }

            val tokenId = UUID.fromString(claims.jwtid)
            val tokenPersistence =
                refreshTokenRepository.findByTokenId(tokenId)
                    ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found")

            if (tokenPersistence.revoked) {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token revoked")
            }

            if (tokenPersistence.used) {
                // Reuse detection!
                revokeAllTokensForUser(tokenPersistence.userId)
                throw ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token already used. Compromise detected.",
                )
            }

            val userId = UUID.fromString(claims.subject)

            // Mark the current token as used
            tokenPersistence.used = true
            refreshTokenRepository.save(tokenPersistence)

            // Generate new token in the same family
            val newRefreshToken = generateRefreshToken(userId, parentId = tokenPersistence.id)

            return userId to newRefreshToken
        } catch (e: Exception) {
            if (e is ResponseStatusException) throw e
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token", e)
        }
    }

    @Transactional
    fun revokeAllTokensForUser(userId: UUID) {
        val tokens = refreshTokenRepository.findByUserId(userId)
        tokens.forEach { it.revoked = true }
        refreshTokenRepository.saveAll(tokens)
    }

    @Transactional
    fun cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now())
    }

    fun getAccessTokenExpirationMillis(): Long = accessTokenExpiration.toMillis()

    fun getRefreshTokenExpiration(): Duration = refreshTokenExpiration
}
