package com.r8n.backend.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
class SecurityReactiveConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun springSecurityFilterChain(
        http: ServerHttpSecurity,
        @Value("\${r8n.security.public-paths:}") publicPaths: Array<String>?,
    ): SecurityWebFilterChain {
        val paths = publicPaths ?: emptyArray()

        return http
            .csrf { it.disable() } // CSRF handled by downstream services or disabled for Gateway
            .authorizeExchange { exchange ->
                if (paths.isNotEmpty()) {
                    exchange.pathMatchers(*paths).permitAll()
                }
                // Allow public API paths if configured
                exchange.pathMatchers("/api/public/**").permitAll()
                exchange.anyExchange().permitAll() // By default, Gateway is transparent
            }.httpBasic { it.disable() }
            .formLogin { it.disable() }
            .build()
    }
}
