package com.r8n.backend.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityServletConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun roleHierarchy(): RoleHierarchy =
        RoleHierarchyImpl.fromHierarchy(
            "ROLE_ADMIN > ROLE_SUPPORT > ROLE_MODERATOR > ROLE_USER",
        )

    @Bean
    @ConditionalOnMissingBean
    fun methodSecurityExpressionHandler(roleHierarchy: RoleHierarchy): MethodSecurityExpressionHandler =
        DefaultMethodSecurityExpressionHandler().also { it.setRoleHierarchy(roleHierarchy) }

    @Bean
    @ConditionalOnMissingBean
    fun maskingLoggingFilter(): MaskingLoggingFilter = MaskingLoggingFilter()

    @Bean
    @ConditionalOnMissingBean
    fun restSecurityInterceptor(serviceTokenService: ServiceTokenService): RestSecurityInterceptor =
        RestSecurityInterceptor(serviceTokenService)

    @Bean
    @ConditionalOnMissingBean
    fun filterChain(
        http: HttpSecurity,
        jwtDecoder: JwtDecoder,
        jwtAuthenticationConverter: JwtAuthenticationConverter,
        maskingLoggingFilter: MaskingLoggingFilter,
        @Value("\${r8n.security.public-paths:}") publicPaths: Array<String>?,
        @Value("\${server.ssl.enabled:false}") sslEnabled: Boolean,
    ): SecurityFilterChain {
        val csrfHandler = CsrfTokenRequestAttributeHandler()
        // Set the name of the attribute the CsrfToken will be populated on
        csrfHandler.setCsrfRequestAttributeName("_csrf")

        val paths = publicPaths ?: emptyArray()

        return http
            .csrf { csrf ->
                csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(csrfHandler)
                    .ignoringRequestMatchers(
                        *paths
                            .filter { path ->
                                !path.startsWith("/api/auth/") && path != "/api/auth/**"
                            }.toTypedArray(),
                    )
            }.headers { headers ->
                headers
                    .contentSecurityPolicy { csp ->
                        csp.policyDirectives("default-src 'self'; frame-ancestors 'none';")
                    }.referrerPolicy { referrer ->
                        referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                    }.httpStrictTransportSecurity { hsts ->
                        if (sslEnabled) {
                            hsts.includeSubDomains(true).maxAgeInSeconds(31536000)
                        } else {
                            hsts.disable()
                        }
                    }.permissionsPolicyHeader { permissions ->
                        permissions.policy("geolocation=(), microphone=(), camera=()")
                    }
            }.addFilterBefore(
                maskingLoggingFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            ).authorizeHttpRequests { auth ->
                if (paths.isNotEmpty()) {
                    auth.requestMatchers(*paths).permitAll()
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
