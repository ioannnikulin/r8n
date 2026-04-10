package com.r8n.backend.opinions

import com.r8n.backend.mock.stub.OpinionSubjectTestDataFactory.bernardReferent
import com.r8n.backend.mock.stub.OpinionSubjectTestDataFactory.cappuccino1A
import com.r8n.backend.opinions.api.dto.OpinionDto
import com.r8n.backend.opinions.api.dto.OpinionStatusEnumDto
import com.r8n.backend.users.integration.api.UsersInternalApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.Instant
import java.util.UUID

@ActiveProfiles("test")
@Testcontainers
@AutoConfigureJsonTesters
@AutoConfigureMockMvc
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@Import(TestObjectMapperConfiguration::class)
class OpinionsIntegrationTests {
    private companion object {
        @Container
        @ServiceConnection
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer(DockerImageName.parse("postgres:15"))
                .withDatabaseName("opinions")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("db/init-schema.sql")
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var usersInternalApi: UsersInternalApi

    @BeforeEach
    fun setUp() {
        whenever(usersInternalApi.getUserName(eq(bernardReferent.id)))
            .thenReturn(bernardReferent.name)
    }

    @Test
    fun `get opinion works`() {
        val requestedId = "30000000-0000-0000-0000-000000000001"
        val result = mockMvc.perform(
            get("/opinions/$requestedId")
                .with(jwt())
        )
            .andExpect(status().isOk).andReturn()

        val actual: OpinionDto = objectMapper.readValue(result.response.contentAsString)
        val expected =
            OpinionDto(
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                bernardReferent.id,
                bernardReferent.name,
                cappuccino1A.id,
                cappuccino1A.name,
                listOf("reminds of grandma's home coffee"),
                listOf("5.50€", "lactose-free milk"),
                4.23,
                null,
                emptyList(),
                OpinionStatusEnumDto.DRAFT,
                Instant.parse("2024-02-01T09:30:00Z"),
            )

        assertEquals(expected, actual)
    }
}