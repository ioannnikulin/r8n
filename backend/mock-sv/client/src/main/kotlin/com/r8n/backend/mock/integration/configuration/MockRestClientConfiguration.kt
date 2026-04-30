package com.r8n.backend.mock.integration.configuration

import com.r8n.backend.mock.api.RecommendationApi
import com.r8n.backend.mock.api.SelectorApi
import com.r8n.backend.mock.integration.client.RecommendationRestClient
import com.r8n.backend.mock.integration.client.SelectorRestClient
import com.r8n.backend.security.SecurityContextTokenInterceptor
import com.r8n.backend.security.ServiceTokenService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class MockRestClientConfiguration {
    @Bean
    @Qualifier("mockRestBaseClient")
    fun mockRestBaseClient(
        @Value("\${services.mock.url}") baseUrl: String,
        serviceTokenService: ServiceTokenService,
    ): RestClient =
        RestClient
            .builder()
            .baseUrl(baseUrl)
            .requestInterceptor(SecurityContextTokenInterceptor(serviceTokenService))
            .build()

    @Bean
    fun recommendationsRestClient(
        @Qualifier("mockRestBaseClient") mockRestBaseClient: RestClient,
    ): RecommendationApi = RecommendationRestClient(mockRestBaseClient)

    @Bean
    fun selectorRestClient(
        @Qualifier("mockRestBaseClient") mockRestBaseClient: RestClient,
    ): SelectorApi = SelectorRestClient(mockRestBaseClient)
}
