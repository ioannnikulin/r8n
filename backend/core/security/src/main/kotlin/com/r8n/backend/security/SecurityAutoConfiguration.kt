package com.r8n.backend.security

import org.springframework.beans.factory.annotation.Value
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityAutoConfiguration {
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
        @Value("\${spring.application.name:unknown-service}") serviceName:
            String,
    ): ServiceTokenService = ServiceTokenService(privateKeyPem, issuer, serviceName)

    @Bean
    @ConditionalOnMissingBean
    fun restSecurityInterceptor(serviceTokenService: ServiceTokenService): RestSecurityInterceptor =
        RestSecurityInterceptor(serviceTokenService)

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
    fun passwordEncoder(): org.springframework.security.crypto.password.PasswordEncoder =
        org.springframework.security.crypto.bcrypt
            .BCryptPasswordEncoder()

    @Bean
    @ConditionalOnMissingBean
    fun maskingLoggingFilter(): MaskingLoggingFilter = MaskingLoggingFilter()

    @Bean
    @ConditionalOnMissingBean
    fun filterChain(
        http: HttpSecurity,
        jwtDecoder: JwtDecoder,
        jwtAuthenticationConverter: JwtAuthenticationConverter,
        maskingLoggingFilter: MaskingLoggingFilter,
        @Value("\${r8n.security.public-paths:}") publicPaths: Array<String>,
    ): SecurityFilterChain {
        val csrfHandler = CsrfTokenRequestAttributeHandler()
        // Set the name of the attribute the CsrfToken will be populated on
        csrfHandler.setCsrfRequestAttributeName("_csrf")

        return http
            .csrf { csrf ->
                csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(csrfHandler)
                    .ignoringRequestMatchers(
                        *publicPaths,
                    )
            }.addFilterBefore(
                maskingLoggingFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            ).authorizeHttpRequests { auth ->
                if (publicPaths.isNotEmpty()) {
                    auth.requestMatchers(*publicPaths).permitAll()
                }
                auth.anyRequest().authenticated()
            }.oauth2ResourceServer { oauth ->
                oauth.jwt { jwt ->
                    jwt.decoder(jwtDecoder)
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }.build()
    }
}