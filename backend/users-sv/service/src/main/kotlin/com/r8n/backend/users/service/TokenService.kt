package com.r8n.backend.users.service

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.r8n.backend.security.ServiceTokenService.Companion.decodePrivateKey
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

@Service
class TokenService(
    @Value("\${r8n.security.jwt.private-key:}") private val privateKeyPem: String,
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

    fun generateAccessToken(userId: UUID, roles: List<String>): String {
        val now = Instant.now()
        val claimsSet = JWTClaimsSet.Builder()
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
        val claimsSet = JWTClaimsSet.Builder()
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

    fun getAccessTokenExpirationMillis(): Long {
        return accessTokenExpiration.toMillis()
    }
}
