package com.r8n.backend.users.integration

import com.r8n.backend.security.RestSecurityInterceptor
import com.r8n.backend.users.integration.api.UsersInternalApi
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class UsersRestClientConfiguration {
    @Bean
    @Qualifier("usersRestClient")
    fun usersRestClient(
        @Value("\${services.users.url}") baseUrl: String,
        restSecurityInterceptor: RestSecurityInterceptor,
    ): RestClient =
        RestClient
            .builder()
            .baseUrl(baseUrl)
            .requestInterceptor(restSecurityInterceptor)
            .build()

    @Bean
    fun userRestClient(
        @Qualifier("usersRestClient") restClient: RestClient,
    ): UsersInternalApi = UsersRestClient(restClient)
}
