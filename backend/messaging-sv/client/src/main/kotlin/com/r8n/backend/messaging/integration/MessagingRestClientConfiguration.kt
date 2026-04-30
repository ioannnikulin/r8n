package com.r8n.backend.messaging.integration

import com.r8n.backend.messaging.api.MessagingApi
import com.r8n.backend.messaging.integration.client.MessagingRestClient
import com.r8n.backend.security.SecurityContextTokenInterceptor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class MessagingRestClientConfiguration {
    @Bean
    @Qualifier("messagingRestBaseClient")
    fun messagingRestBaseClient(
        @Value("\${services.messaging.url}") baseUrl: String,
        serviceTokenService: com.r8n.backend.security.ServiceTokenService,
    ): RestClient =
        RestClient
            .builder()
            .baseUrl(baseUrl)
            .requestInterceptor(SecurityContextTokenInterceptor(serviceTokenService))
            .build()

    @Bean
    fun messagingRestClient(
        @Qualifier("messagingRestBaseClient") messagingRestBaseClient: RestClient,
    ): MessagingApi = MessagingRestClient(messagingRestBaseClient)
}
