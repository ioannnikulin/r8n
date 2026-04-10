package com.r8n.backend.security

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import java.security.interfaces.RSAPublicKey
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityAutoConfiguration {
    companion object {
        const val PUBLIC_KEY_PROPERTY = "r8n.security.jwt.public-key"
    }

    @Bean
    @ConditionalOnMissingBean
    fun serviceTokenService(
        @org.springframework.beans.factory.annotation.Value("\${r8n.security.jwt.private-key:}") privateKeyPem: String,
        @org.springframework.beans.factory.annotation.Value("\${r8n.security.jwt.issuer:r8n}") issuer: String,
        @org.springframework.beans.factory.annotation.Value("\${spring.application.name:unknown-service}") serviceName: String,
    ): ServiceTokenService {
        return ServiceTokenService(privateKeyPem, issuer, serviceName)
    }

    @Bean
    @ConditionalOnMissingBean
    fun restSecurityInterceptor(serviceTokenService: ServiceTokenService): RestSecurityInterceptor {
        return RestSecurityInterceptor(serviceTokenService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun jwtDecoder(
        @org.springframework.beans.factory.annotation.Value("\${${PUBLIC_KEY_PROPERTY}:}") publicKeyPem: String
    ): JwtDecoder {
        if (publicKeyPem.isBlank()) {
            // Fallback for local development if not provided - use a dummy decoder or fail
            // In a real app, this should probably fail or use a default dev key
            // For now, let's use a very simple (unsafe) decoder if key is missing to not break everything immediately
            // But ideally, it should be provided.
            throw IllegalStateException("Public key for JWT verification is not provided in property $PUBLIC_KEY_PROPERTY")
        }

        val publicKey = decodePublicKey(publicKeyPem)
        return NimbusJwtDecoder.withPublicKey(publicKey).build()
    }

    private fun decodePublicKey(pem: String): RSAPublicKey {
        val cleanPem = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        val encoded = Base64.getDecoder().decode(cleanPem)
        val keySpec = X509EncodedKeySpec(encoded)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec) as RSAPublicKey
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
    fun passwordEncoder(): org.springframework.security.crypto.password.PasswordEncoder {
        return org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
    }

    @Bean
    @ConditionalOnMissingBean
    fun maskingLoggingFilter(): MaskingLoggingFilter {
        return MaskingLoggingFilter()
    }

    @Bean
    @ConditionalOnMissingBean
    fun filterChain(http: HttpSecurity, jwtDecoder: JwtDecoder, jwtAuthenticationConverter: JwtAuthenticationConverter, maskingLoggingFilter: MaskingLoggingFilter): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .addFilterBefore(maskingLoggingFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests {
                it.requestMatchers("/auth/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth ->
                oauth.jwt { jwt ->
                    jwt.decoder(jwtDecoder)
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }
            .build()
    }
}