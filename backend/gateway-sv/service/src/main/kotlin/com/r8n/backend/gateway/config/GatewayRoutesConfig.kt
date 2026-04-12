package com.r8n.backend.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GatewayRoutesConfig(
    @Value("\${INTERSERVICE_PROTOCOL:http}") private val protocol: String,
    @Value("\${SERVICES_OPINIONS_HOST:opinions}") private val opinionsHost: String,
    @Value("\${SERVICES_OPINIONS_PORT:8080}") private val opinionsPort: String,
    @Value("\${SERVICES_USERS_HOST:users}") private val usersHost: String,
    @Value("\${SERVICES_USERS_PORT:8080}") private val usersPort: String,
    @Value("\${SERVICES_EXPORT_HOST:export}") private val exportHost: String,
    @Value("\${SERVICES_EXPORT_PORT:8080}") private val exportPort: String,
    @Value("\${SERVICES_MOCK_HOST:mock}") private val mockHost: String,
    @Value("\${SERVICES_MOCK_PORT:8080}") private val mockPort: String,
) {
    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator =
        builder
            .routes()
            .route("opinions") { r ->
                r
                    .path("/opinions/**")
                    .uri("$protocol://$opinionsHost:$opinionsPort")
            }.route("users") { r ->
                r
                    .path("/users/**")
                    .uri("$protocol://$usersHost:$usersPort")
            }.route("export") { r ->
                r
                    .path("/export/**")
                    .uri("$protocol://$exportHost:$exportPort")
            }.route("mock") { r ->
                r
                    .path("/auth/**", "/access-requests/**", "/opinion-lists/**", "/selectors/**")
                    .uri("$protocol://$mockHost:$mockPort")
            }.build()
}