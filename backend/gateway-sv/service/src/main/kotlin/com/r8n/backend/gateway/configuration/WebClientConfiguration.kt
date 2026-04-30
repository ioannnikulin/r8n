package com.r8n.backend.gateway.configuration

import com.r8n.backend.security.ServiceTokenService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Configuration
class WebClientConfiguration {
/*
Other services own their integration clients based on blocking RestClient.
Gateway is reactive and needs WebClient for communication, so to avoid confusion, it owns its client to users-sv itself.
 */

    @Bean
    fun webClientBuilder(): WebClient.Builder = WebClient.builder()

    @Bean
    fun usersWebClient(
        @Value("\${interservice.protocol}") protocol: String,
        @Value("\${services.users.host}") host: String,
        @Value("\${services.users.port}") port: Int,
        serviceTokenService: ServiceTokenService,
        webClientBuilder: WebClient.Builder,
    ): WebClient =
        webClientBuilder
            .baseUrl("$protocol://$host:$port")
            .filter(serviceTokenFilter(serviceTokenService))
            .build()

    private fun serviceTokenFilter(serviceTokenService: ServiceTokenService): ExchangeFilterFunction =
        ExchangeFilterFunction.ofRequestProcessor { request: ClientRequest ->
            val token = serviceTokenService.generateServiceToken()
            val newRequest =
                if (token != null) {
                    ClientRequest
                        .from(request)
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    request
                }
            Mono.just(newRequest)
        }
}
