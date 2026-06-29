package com.r8n.backend.opinions.lists

import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.TestObjectMapperConfiguration
import com.r8n.backend.opinions.api.lists.dto.OpinionListSummaryDto
import com.r8n.backend.security.ServiceTokenService
import com.r8n.backend.users.integration.api.UsersInternalApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters
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
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.util.UUID

@ActiveProfiles("test")
@Testcontainers
@AutoConfigureJsonTesters
@AutoConfigureMockMvc
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@Import(TestObjectMapperConfiguration::class)
class OpinionListPrivacyLeakIntegrationTest {
    companion object {
        val ANNA_ID: UUID = UUID.fromString("20202020-2020-2020-2020-202020202020")

        val PRIVATE_UNAPPROVED =
            listOf(
                UUID.fromString("80000000-0000-0000-0000-000000000231"),
                UUID.fromString("80000000-0000-0000-0000-000000000232"),
                UUID.fromString("80000000-0000-0000-0000-000000000235"),
                UUID.fromString("80000000-0000-0000-0000-000000000234"),
            )

        val SEARCHABLE_UNAPPROVED =
            listOf(
                UUID.fromString("80000000-0000-0000-0000-000000000227"),
                UUID.fromString("80000000-0000-0000-0000-000000000228"),
                UUID.fromString("80000000-0000-0000-0000-000000000229"),
                UUID.fromString("80000000-0000-0000-0000-000000000230"),
            )

        @Suppress("unused")
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
    lateinit var objectMapper: JsonMapper

    @Autowired
    lateinit var serviceTokenService: ServiceTokenService

    @MockitoBean
    lateinit var usersInternalApi: UsersInternalApi

    lateinit var annaToken: String

    @BeforeEach
    fun setUp() {
        annaToken = "Bearer " + serviceTokenService.generateAccessToken(ANNA_ID, listOf("USER"))
        whenever(usersInternalApi.isAnyModerator(any())).thenReturn(false)
        whenever(usersInternalApi.getUserName(any())).thenReturn("Some User")
    }

    @Test
    fun `user should not see private lists of others in search`() {
        val result =
            mockMvc
                .perform(
                    get("/api/opinion-lists/search")
                        .header("Authorization", annaToken)
                        .param("nameSubstring", "l2")
                        .param("page", "0")
                        .param("size", "10"),
                ).andExpect(status().isOk)
                .andReturn()

        val page = objectMapper.readValue<PageResponseDto<OpinionListSummaryDto>>(result.response.contentAsString)
        val names = page.items.map { it.listName }
        assertThat(names).contains("l21", "l22", "l23")
        assertThat(names).doesNotContain("l24")
        // Anna has access to it already, but then the list was made private, and now she can't see it in discovery
    }

    @Test
    fun `user should not be able to get summary of a list without access`() {
        for (id in PRIVATE_UNAPPROVED) {
            mockMvc
                .perform(
                    get("/api/opinion-lists/$id/summary")
                        .header("Authorization", annaToken),
                ).andExpect(status().isNotFound)
        }
        for (id in SEARCHABLE_UNAPPROVED) {
            mockMvc
                .perform(
                    get("/api/opinion-lists/$id/summary")
                        .header("Authorization", annaToken),
                ).andExpect(status().isForbidden)
        }
    }

    @Test
    fun `user should not be able to get content of a list without access`() {
        for (id in PRIVATE_UNAPPROVED) {
            mockMvc
                .perform(
                    get("/api/opinion-lists/$id")
                        .header("Authorization", annaToken),
                ).andExpect(status().isNotFound)
        }
        for (id in SEARCHABLE_UNAPPROVED) {
            mockMvc
                .perform(
                    get("/api/opinion-lists/$id")
                        .header("Authorization", annaToken),
                ).andExpect(status().isForbidden)
        }
    }
}
