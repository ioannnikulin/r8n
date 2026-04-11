package com.r8n.backend.users.service

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.r8n.backend.security.SecurityAutoConfiguration.Companion.decodePublicKey
import com.r8n.backend.security.ServiceTokenService.Companion.decodePrivateKey
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

@Service
class TokenService(
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

    fun generateRefreshToken(userId: UUID): String {
        val now = Instant.now()
        val claimsSet =
            JWTClaimsSet
                .Builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim("refresh", true)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(refreshTokenExpiration)))
                .build()

        val signedJWT = SignedJWT(JWSHeader(JWSAlgorithm.RS256), claimsSet)
        signedJWT.sign(signer)
        return signedJWT.serialize()
    }

    fun validateRefreshToken(refreshToken: String): UUID {
        try {
            val signedJWT = SignedJWT.parse(refreshToken)
            val pubKey = decodePublicKey(publicKeyPem)
            val verifier =
                com.nimbusds.jose.crypto
                    .RSASSAVerifier(pubKey)

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

            return UUID.fromString(claims.subject)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token", e)
        }
    }

    fun getAccessTokenExpirationMillis(): Long = accessTokenExpiration.toMillis()
}