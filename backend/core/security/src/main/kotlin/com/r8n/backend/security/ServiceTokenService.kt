package com.r8n.backend.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Date

@Service
class ServiceTokenService(
    @Value("\${r8n.security.jwt.private-key:}") private val privateKeyPem: String,
    @Value("\${r8n.security.jwt.issuer:r8n}") private val issuer: String,
    @Value("\${r8n.security.service-name:unknown-service}") private val serviceName: String,
) {
    private val signer: JWSSigner? by lazy {
        if (privateKeyPem.isBlank()) {
            null
        } else {
            RSASSASigner(decodePrivateKey(privateKeyPem))
        }
    }

    fun generateServiceToken(): String? {
        val s = signer ?: return null
        return generateToken(s, "service-$serviceName", listOf("SERVICE"), Duration.ofMinutes(5))
    }

    fun generateAccessToken(
        userId: java.util.UUID,
        roles: List<String>,
    ): String? {
        val s = signer ?: return null
        return generateToken(s, userId.toString(), roles, Duration.ofHours(1))
    }

    private fun generateToken(
        signer: JWSSigner,
        subject: String,
        roles: List<String>,
        expiration: Duration,
    ): String {
        val now = Instant.now()
        val claimsSet =
            JWTClaimsSet
                .Builder()
                .issuer(issuer)
                .subject(subject)
                .claim("roles", roles)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(expiration)))
                .build()

        val signedJWT = SignedJWT(JWSHeader(JWSAlgorithm.RS256), claimsSet)
        signedJWT.sign(signer)
        return signedJWT.serialize()
    }

    companion object {
        fun decodePrivateKey(pem: String): PrivateKey {
            val cleanPem =
                pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("\\s".toRegex(), "")
            val encoded = Base64.getDecoder().decode(cleanPem)
            val keySpec = PKCS8EncodedKeySpec(encoded)
            val keyFactory = KeyFactory.getInstance("RSA")
            return keyFactory.generatePrivate(keySpec)
        }
    }
}