package com.r8n.backend.messaging

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
class MessagingApplicationTests {
    private companion object {
        @Container
        @ServiceConnection
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer(DockerImageName.parse("postgres:15"))
                .withDatabaseName("messaging")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("db/init-schema.sql")
    }

    @Test
    fun contextLoads() {
    }
}