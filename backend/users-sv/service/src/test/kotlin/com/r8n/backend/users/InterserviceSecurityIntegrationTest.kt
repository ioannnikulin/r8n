package com.r8n.backend.users

import com.r8n.backend.opinions.api.access.IncomingAccessRequestApi
import com.r8n.backend.opinions.api.access.OutgoingAccessRequestApi
import com.r8n.backend.opinions.integration.api.OpinionListsInternalApi
import com.r8n.backend.security.ServiceTokenService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID

@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestObjectMapperConfiguration::class)
@MockitoBean(
    types = [
        IncomingAccessRequestApi::class,
        OutgoingAccessRequestApi::class,
        OpinionListsInternalApi::class,
    ],
)
class InterserviceSecurityIntegrationTest {
    companion object {
        @Suppress("unused") // used to store test database container
        @Container
        @ServiceConnection
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer("postgres:15")
                .withDatabaseName("users")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("db/init-schema.sql")
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var serviceTokenService: ServiceTokenService

    @Test
    fun `getUserName requires authentication`() {
        val userId = UUID.randomUUID()
        mockMvc
            .perform(get("/api/internal/users/$userId/name"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `getUserName allows access with SERVICE role`() {
        val userId = UUID.randomUUID()
        val token = serviceTokenService.generateServiceToken()

        mockMvc
            .perform(
                get("/api/internal/users/$userId/name")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isOk)
    }

    @Test
    fun `getUserName allows access with USER role`() {
        val userId = UUID.randomUUID()
        val token = serviceTokenService.generateAccessToken(UUID.randomUUID(), listOf("USER"))

        mockMvc
            .perform(
                get("/api/internal/users/$userId/name")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isOk)
    }
}
