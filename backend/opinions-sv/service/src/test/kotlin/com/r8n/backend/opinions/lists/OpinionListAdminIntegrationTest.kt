package com.r8n.backend.opinions.lists

import com.r8n.backend.opinions.TestObjectMapperConfiguration
import com.r8n.backend.opinions.api.lists.dto.OpinionListDto
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestObjectMapperConfiguration::class)
class OpinionListAdminIntegrationTest {
    private companion object {
        val ANNA_ID: UUID = UUID.fromString("20202020-2020-2020-2020-202020202020")
        val BERNARD_ID: UUID = UUID.fromString("10101010-1010-1010-1010-101010101010")
        val ANNA_L11_ID: UUID = UUID.fromString("80000000-0000-0000-0000-000000000111")
        val BERNARD_L21_ID: UUID = UUID.fromString("80000000-0000-0000-0000-000000000211")
        val MISSING_LIST_ID: UUID = UUID.fromString("99999999-9999-9999-9999-999999999999")

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
    lateinit var bernardToken: String

    @BeforeEach
    fun setUp() {
        annaToken = "Bearer " + serviceTokenService.generateAccessToken(ANNA_ID, listOf("USER"))
        bernardToken = "Bearer " + serviceTokenService.generateAccessToken(BERNARD_ID, listOf("USER"))
        whenever(usersInternalApi.isAnyModerator(any())).thenReturn(false)
        whenever(usersInternalApi.isHumanModerator(any())).thenReturn(false)
        whenever(usersInternalApi.getUserName(any())).thenReturn("Anna Müller")
    }

    // ---------- changePrivacy ----------

    @Test
    fun `owner can change list privacy and the change is persisted`() {
        val createdId = createList("privacy-target", "PRIVATE")

        mockMvc
            .perform(
                patch("/api/opinion-lists/$createdId/set-privacy")
                    .header("Authorization", annaToken)
                    .param("privacy", "SEARCHABLE"),
            ).andExpect(status().isOk)

        val refetched = getList(createdId, annaToken)
        assertThat(refetched.privacy.toString()).isEqualTo("SEARCHABLE")

        // Flip back to PRIVATE — confirm both directions persist
        mockMvc
            .perform(
                patch("/api/opinion-lists/$createdId/set-privacy")
                    .header("Authorization", annaToken)
                    .param("privacy", "PRIVATE"),
            ).andExpect(status().isOk)

        val refetchedAgain = getList(createdId, annaToken)
        assertThat(refetchedAgain.privacy.toString()).isEqualTo("PRIVATE")
    }

    @Test
    fun `non-owner cannot change list privacy`() {
        mockMvc
            .perform(
                patch("/api/opinion-lists/$BERNARD_L21_ID/set-privacy")
                    .header("Authorization", annaToken)
                    .param("privacy", "PRIVATE"),
            ).andExpect(status().isForbidden)
    }

    // ---------- renameList ----------

    @Test
    fun `owner can rename list and the change is persisted`() {
        val createdId = createList("original-name", "PRIVATE")

        mockMvc
            .perform(
                patch("/api/opinion-lists/$createdId/rename")
                    .header("Authorization", annaToken)
                    .param("name", "renamed-list"),
            ).andExpect(status().isOk)

        val refetched = getList(createdId, annaToken)
        assertThat(refetched.listName).isEqualTo("renamed-list")
    }

    @Test
    fun `rename rejects blank or whitespace-only name`() {
        val createdId = createList("rename-blank-target", "PRIVATE")

        mockMvc
            .perform(
                patch("/api/opinion-lists/$createdId/rename")
                    .header("Authorization", annaToken)
                    .param("name", "   "),
            ).andExpect(status().is4xxClientError)
    }

    @Test
    fun `non-owner cannot rename list`() {
        mockMvc
            .perform(
                patch("/api/opinion-lists/$ANNA_L11_ID/rename")
                    .header("Authorization", bernardToken)
                    .param("name", "hijacked"),
            ).andExpect(status().isForbidden)
    }

    // ---------- deleteList ----------

    @Test
    fun `owner can delete their list and follow-up GET returns 404`() {
        val createdId = createList("doomed-list", "PRIVATE")

        mockMvc
            .perform(
                delete("/api/opinion-lists/$createdId")
                    .header("Authorization", annaToken),
            ).andExpect(status().is2xxSuccessful)

        mockMvc
            .perform(
                get("/api/opinion-lists/$createdId")
                    .header("Authorization", annaToken),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `non-owner cannot delete a list`() {
        mockMvc
            .perform(
                delete("/api/opinion-lists/$ANNA_L11_ID")
                    .header("Authorization", bernardToken),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `delete on missing list returns 4xx`() {
        mockMvc
            .perform(
                delete("/api/opinion-lists/$MISSING_LIST_ID")
                    .header("Authorization", annaToken),
            ).andExpect(status().is4xxClientError)
    }

    // ---------- helpers ----------

    private fun createList(
        name: String,
        privacy: String,
    ): UUID {
        val response =
            mockMvc
                .perform(
                    post("/api/opinion-lists")
                        .header("Authorization", annaToken)
                        .param("name", name)
                        .param("privacy", privacy),
                ).andExpect(status().isOk)
                .andReturn()
                .response.contentAsString
        return objectMapper.readValue<OpinionListDto>(response).id
    }

    private fun getList(
        listId: UUID,
        token: String,
    ): OpinionListDto {
        val response =
            mockMvc
                .perform(
                    get("/api/opinion-lists/$listId")
                        .header("Authorization", token),
                ).andExpect(status().isOk)
                .andReturn()
                .response.contentAsString
        return objectMapper.readValue(response)
    }
}
