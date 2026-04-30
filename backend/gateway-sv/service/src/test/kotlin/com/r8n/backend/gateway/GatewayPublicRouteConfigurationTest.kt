package com.r8n.backend.gateway

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class GatewayPublicRouteConfigurationTest {
    @Test
    fun `public subjects route is exposed with api key protection in every gateway config`() {
        val gatewayConfigs =
            listOf(
                Path.of("src/main/resources/application.yml"),
                Path.of("src/main/resources/application-local.yml"),
                Path.of("src/test/resources/application-test.yml"),
                Path.of("src/test/resources/application-test-e2e.yml"),
            )

        gatewayConfigs.forEach { config ->
            val content = Files.readString(config)

            assertThat(content)
                .describedAs("$config exposes subjects through the public API namespace")
                .contains("id: public_subjects")
                .contains("Path=/api/public/subjects,/api/public/subjects/**")
                .contains("ApiKeyAuthenticationFilter")
                .contains("RateLimitingFilter")
                .contains("RewritePath=/api/public/subjects/?(?<segment>.*), /api/subjects/$\\{segment}")
        }
    }
}
