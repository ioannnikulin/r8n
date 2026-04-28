package com.r8n.backend.messaging

import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.core.utils.TestObjectMapperConfiguration
import com.r8n.backend.messaging.api.MessagingApi.Companion.DIRECT_THREADS_PATH
import com.r8n.backend.messaging.api.MessagingApi.Companion.SUPPORT_THREADS_PATH
import com.r8n.backend.messaging.api.MessagingApi.Companion.THREADS_PATH
import com.r8n.backend.messaging.api.dto.MessageDto
import com.r8n.backend.messaging.api.dto.ThreadSummaryDto
import com.r8n.backend.messaging.api.dto.ThreadTypeEnumDto
import com.r8n.backend.users.integration.api.UsersInternalApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.UUID

@ActiveProfiles("test")
@Testcontainers
@AutoConfigureJsonTesters
@AutoConfigureMockMvc
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
)
@Import(TestObjectMapperConfiguration::class)
class MessagingControllerIntegrationTests {
    private companion object {
        val USER_A: UUID = UUID.fromString("10000000-0000-0000-0000-000000000001")
        val USER_B: UUID = UUID.fromString("10000000-0000-0000-0000-000000000002")
        val USER_C: UUID = UUID.fromString("10000000-0000-0000-0000-000000000003")

        @Container
        @ServiceConnection
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer(DockerImageName.parse("postgres:15"))
                .withDatabaseName("messaging")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("db/init-schema.sql")
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @MockitoBean
    lateinit var usersInternalApi: UsersInternalApi

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update("DELETE FROM messaging.messages")
        jdbcTemplate.update("DELETE FROM messaging.threads")
        whenever(usersInternalApi.getUserName(any())).thenAnswer { invocation ->
            when (invocation.getArgument<UUID>(0)) {
                USER_A -> "Alice Reviewer"
                USER_B -> "Bob Reviewer"
                USER_C -> "Carol Reviewer"
                else -> "Unknown User"
            }
        }
    }

    @Test
    fun `messaging endpoints require authenticated user`() {
        mockMvc
            .perform(get(THREADS_PATH).queryParam("page", "0").queryParam("size", "10"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `service role cannot access user messaging endpoints`() {
        mockMvc
            .perform(
                get(THREADS_PATH)
                    .queryParam("page", "0")
                    .queryParam("size", "10")
                    .with(jwtFor(USER_A, "SERVICE")),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `support thread can be created listed read and loaded`() {
        val created =
            postJson(
                SUPPORT_THREADS_PATH,
                """{"initialMessage":"Need help"}""",
                USER_A,
            ).andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
                .let { objectMapper.readValue<ThreadSummaryDto>(it) }

        assertEquals(ThreadTypeEnumDto.SUPPORT, created.type)
        assertEquals("R8N Support", created.participant.name)
        assertEquals("Need help", created.lastMessagePreview)
        assertEquals(0, created.unreadCount)

        val listed = getThreads(USER_A).items.single()
        assertEquals(created.id, listed.id)

        val messages = getMessages(USER_A, created.id).items
        assertEquals(listOf("Need help"), messages.map { it.text })
        assertEquals(listOf(true), messages.map { it.own })

        val read =
            postJson(
                "/api/messaging/threads/${created.id}/read",
                null,
                USER_A,
            ).andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
                .let { objectMapper.readValue<ThreadSummaryDto>(it) }

        assertEquals(0, read.unreadCount)
    }

    @Test
    fun `direct thread can be created sent listed and marked read`() {
        val thread =
            postJson(
                DIRECT_THREADS_PATH,
                """{"recipientUserId":"$USER_B","initialMessage":"Hello Bob"}""",
                USER_A,
            ).andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
                .let { objectMapper.readValue<ThreadSummaryDto>(it) }

        assertEquals(ThreadTypeEnumDto.DIRECT, thread.type)
        assertEquals(USER_B, thread.participant.userId)
        assertEquals("Bob Reviewer", thread.participant.name)

        val visibleToRecipient = getThreads(USER_B).items.single()
        assertEquals(thread.id, visibleToRecipient.id)
        assertEquals(USER_A, visibleToRecipient.participant.userId)
        assertEquals("Alice Reviewer", visibleToRecipient.participant.name)
        assertEquals(1, visibleToRecipient.unreadCount)

        postJson(
            "/api/messaging/threads/${thread.id}/messages",
            """{"text":"Hi Alice"}""",
            USER_B,
        ).andExpect(status().isOk)

        val messages = getMessages(USER_A, thread.id).items
        assertEquals(listOf("Hello Bob", "Hi Alice"), messages.map { it.text })
        assertEquals(listOf(true, false), messages.map { it.own })

        val read =
            postJson(
                "/api/messaging/threads/${thread.id}/read",
                null,
                USER_A,
            ).andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
                .let { objectMapper.readValue<ThreadSummaryDto>(it) }

        assertEquals(0, read.unreadCount)
    }

    @Test
    fun `non participant gets not found for private thread messages`() {
        val thread =
            postJson(
                DIRECT_THREADS_PATH,
                """{"recipientUserId":"$USER_B","initialMessage":"Private"}""",
                USER_A,
            ).andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
                .let { objectMapper.readValue<ThreadSummaryDto>(it) }

        mockMvc
            .perform(
                get("/api/messaging/threads/${thread.id}/messages")
                    .queryParam("page", "0")
                    .queryParam("size", "10")
                    .with(jwtFor(USER_C)),
            ).andExpect(status().isNotFound)
    }

    private fun postJson(
        path: String,
        body: String?,
        userId: UUID,
    ) = mockMvc.perform(
        post(path)
            .with(csrf())
            .with(jwtFor(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .apply {
                if (body != null) {
                    content(body)
                }
            },
    )

    private fun getThreads(userId: UUID): PageResponseDto<ThreadSummaryDto> =
        mockMvc
            .perform(
                get(THREADS_PATH)
                    .queryParam("page", "0")
                    .queryParam("size", "10")
                    .with(jwtFor(userId)),
            ).andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readValue(it) }

    private fun getMessages(
        userId: UUID,
        threadId: UUID,
    ): PageResponseDto<MessageDto> =
        mockMvc
            .perform(
                get("/api/messaging/threads/$threadId/messages")
                    .queryParam("page", "0")
                    .queryParam("size", "10")
                    .with(jwtFor(userId)),
            ).andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readValue(it) }

    private fun jwtFor(
        userId: UUID,
        role: String = "USER",
    ) = jwt()
        .jwt { token -> token.subject(userId.toString()) }
        .authorities(SimpleGrantedAuthority("ROLE_$role"))
}