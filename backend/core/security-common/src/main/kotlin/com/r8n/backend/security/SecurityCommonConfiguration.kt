package com.r8n.backend.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Configuration
class SecurityCommonConfiguration {
    companion object {
        const val PUBLIC_KEY_PROPERTY = "r8n.security.jwt.public-key"

        fun decodePublicKey(pem: String): RSAPublicKey {
            val cleanPem =
                pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                    .replace("-----END RSA PUBLIC KEY-----", "")
                    .replace("\\s".toRegex(), "")
            val encoded = Base64.getDecoder().decode(cleanPem)
            val keySpec = X509EncodedKeySpec(encoded)
            val keyFactory = KeyFactory.getInstance("RSA")
            return keyFactory.generatePublic(keySpec) as RSAPublicKey
        }
    }

    @Bean
    @ConditionalOnMissingBean
    fun serviceTokenService(
        @Value("\${r8n.security.jwt.private-key:}") privateKeyPem: String,
        @Value("\${r8n.security.jwt.issuer:r8n}") issuer: String,
        @Value("\${spring.application.name:unknown-service}") serviceName: String,
    ): ServiceTokenService = ServiceTokenService(privateKeyPem, issuer, serviceName)

    @Bean
    @ConditionalOnMissingBean
    fun jwtDecoder(
        @Value("\${${PUBLIC_KEY_PROPERTY}:}") publicKeyPem: String,
    ): JwtDecoder {
        if (publicKeyPem.isBlank()) {
            throw IllegalStateException(
                "Public key for JWT verification is not provided in property $PUBLIC_KEY_PROPERTY. " +
                    "Please ensure it is set in application.yml or environment variables.",
            )
        }

        val publicKey = decodePublicKey(publicKeyPem)
        return NimbusJwtDecoder.withPublicKey(publicKey).build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val grantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles")
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_")

        val jwtAuthenticationConverter = JwtAuthenticationConverter()
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter)
        return jwtAuthenticationConverter
    }

    @Bean
    @ConditionalOnMissingBean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
