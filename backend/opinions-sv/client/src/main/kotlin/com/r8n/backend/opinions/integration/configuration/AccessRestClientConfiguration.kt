package com.r8n.backend.opinions.integration.configuration

import com.r8n.backend.opinions.api.access.IncomingAccessRequestApi
import com.r8n.backend.opinions.api.access.OutgoingAccessRequestApi
import com.r8n.backend.opinions.integration.client.IncomingAccessRequestRestClient
import com.r8n.backend.opinions.integration.client.OutgoingAccessRequestRestClient
import com.r8n.backend.security.SecurityContextTokenInterceptor
import com.r8n.backend.security.ServiceTokenService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class AccessRestClientConfiguration {
    @Bean
    @Qualifier("accessRestBaseClient")
    fun accessRestBaseClient(
        @Value("\${services.opinions.url}") baseUrl: String,
        serviceTokenService: ServiceTokenService,
    ): RestClient =
        RestClient
            .builder()
            .baseUrl(baseUrl)
            .requestInterceptor(SecurityContextTokenInterceptor(serviceTokenService))
            .build()

    @Bean
    fun incomingAccessRequestsRestClient(
        @Qualifier("accessRestBaseClient") accessRestBaseClient: RestClient,
    ): IncomingAccessRequestApi = IncomingAccessRequestRestClient(accessRestBaseClient)

    @Bean
    fun outgoingAccessRequestsRestClient(
        @Qualifier("accessRestBaseClient") accessRestBaseClient: RestClient,
    ): OutgoingAccessRequestApi = OutgoingAccessRequestRestClient(accessRestBaseClient)
}
