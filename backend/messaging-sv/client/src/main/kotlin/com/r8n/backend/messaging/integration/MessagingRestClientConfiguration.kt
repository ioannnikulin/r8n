package com.r8n.backend.messaging.integration

import com.r8n.backend.messaging.integration.api.MessagingInternalApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MessagingRestClientConfiguration {
    @Bean
    fun messagingRestClient(): MessagingInternalApi = MessagingRestClient()
}