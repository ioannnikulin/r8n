package com.r8n.backend.gateway.filter

import com.r8n.backend.security.ServiceTokenService
import com.r8n.backend.users.integration.api.KeyValidationApi
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.UUID

@Component
class ApiKeyAuthenticationFilter(
    private val usersWebClient: WebClient,
    private val serviceTokenService: ServiceTokenService,
) : AbstractGatewayFilterFactory<ApiKeyAuthenticationFilter.Config>(Config::class.java) {
    private val log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter::class.java)

    class Config

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
            val apiKey = exchange.request.headers.getFirst("X-API-KEY")

            if (apiKey == null) {
                log.warn("Missing X-API-KEY header")
                exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                return@GatewayFilter exchange.response.setComplete()
            }

            usersWebClient
                .get()
                .uri(KeyValidationApi.VALIDATE_API_KEY_PATH.replace("{key}", apiKey))
                .retrieve()
                .onStatus({ it.isError }) { response ->
                    response.bodyToMono(String::class.java).defaultIfEmpty("No error body").flatMap { body ->
                        Mono.error(RuntimeException("Validation failed with status ${response.statusCode()}: $body"))
                    }
                }.bodyToMono(UUID::class.java)
                .flatMap { userId ->
                    // Inject a short-lived internal JWT token so downstream services can identify the user
                    val internalToken = serviceTokenService.generateAccessToken(userId, listOf("USER"))

                    val request =
                        exchange.request
                            .mutate()
                            .header("Authorization", "Bearer $internalToken")
                            .build()
                    chain.filter(exchange.mutate().request(request).build())
                }.onErrorResume { e ->
                    log.error("API Key validation failed: {}", e.message)
                    if (e is RuntimeException && e.message?.contains("401") == true) {
                        // If the call to users-sv returned 401, it means the API key is invalid
                        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                    } else {
                        // Other errors (like 500 from users-sv or connection issues) are internal errors
                        exchange.response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
                    }
                    exchange.response.setComplete()
                }
        }
    }
}
