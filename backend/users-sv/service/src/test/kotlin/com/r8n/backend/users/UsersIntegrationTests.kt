package com.r8n.backend.users

import com.r8n.backend.core.utils.toResponse
import com.r8n.backend.mock.api.IncomingAccessRequestApi
import com.r8n.backend.mock.api.MessagingApi
import com.r8n.backend.mock.api.OutgoingAccessRequestApi
import com.r8n.backend.mock.integration.api.OpinionListInternalApi
import com.r8n.backend.mock.stub.AccessRequestsTestDataFactory
import com.r8n.backend.mock.stub.MiscTestFactory
import com.r8n.backend.mock.stub.OpinionListTestDataFactory
import com.r8n.backend.users.api.dto.ConsentDto
import com.r8n.backend.users.api.dto.PersonalIdentifiableInformationSectionDto
import com.r8n.backend.users.api.dto.UserCompleteDataDto
import com.r8n.backend.users.api.dto.UserSessionDto
import com.r8n.backend.users.api.dto.UserStatusEnumDto
import com.r8n.backend.users.service.TokenService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.security.test.context.support.WithMockUser
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
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

@ActiveProfiles("test")
@Testcontainers
@AutoConfigureJsonTesters
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestObjectMapperConfiguration::class)
class UsersIntegrationTests {
    private companion object {
        @Container
        @ServiceConnection
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer(DockerImageName.parse("postgres:15"))
                .withDatabaseName("users")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("db/init-schema.sql")

        const val USER_ID = "00000000-0000-0000-0000-000000000000"
        val opinions = OpinionListTestDataFactory.getList()
        val incomingAccessRequests = AccessRequestsTestDataFactory.get()
        val outgoingAccessRequests = AccessRequestsTestDataFactory.get()
        val supportMessages = MiscTestFactory.getSupportMessage()
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var tokenService: TokenService

    @MockitoBean
    lateinit var opinionClient: OpinionListInternalApi

    @MockitoBean
    lateinit var incomingAccessRequestClient: IncomingAccessRequestApi

    @MockitoBean
    lateinit var outgoingAccessRequestClient: OutgoingAccessRequestApi

    @MockitoBean
    lateinit var messageClient: MessagingApi

    @BeforeEach
    fun setUp() {
        whenever(opinionClient.getMineFull(any())).thenReturn(
            PageImpl(listOf(opinions)).toResponse(),
        )
        whenever(incomingAccessRequestClient.get(anyOrNull(), anyOrNull(), anyOrNull(), any())).thenReturn(
            PageImpl(listOf(incomingAccessRequests)).toResponse(),
        )
        whenever(outgoingAccessRequestClient.get(anyOrNull(), anyOrNull(), anyOrNull(), any())).thenReturn(
            PageImpl(listOf(outgoingAccessRequests)).toResponse(),
        )
        whenever(messageClient.getSupportThreads()).thenReturn(
            PageImpl(listOf(supportMessages)).toResponse(),
        )
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `exportAll returns complete user data`() {
        val accessToken = tokenService.generateAccessToken(UUID.fromString(USER_ID), listOf("USER"))
        val result = mockMvc.perform(
            get("/users/export")
                .header("Authorization", "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andReturn()

        val actual: UserCompleteDataDto = objectMapper.readValue(result.response.contentAsString)

        val timestamp = LocalDateTime.of(2024, 1, 1, 12, 0).toInstant(ZoneOffset.UTC)

        val session =
            UserSessionDto(
                UUID.fromString("01010101-0101-0101-0101-010101010101"),
                timestamp,
                timestamp.plus(
                    1,
                    ChronoUnit.DAYS,
                ),
                "127.0.0.1",
                "Test User Agent",
            )

        val expected =
            UserCompleteDataDto(
                UUID.fromString(USER_ID),
                UserStatusEnumDto.ACTIVE,
                timestamp,
                PageImpl(
                    listOf(
                        ConsentDto("PRIVACY_POLICY", timestamp, session),
                    ),
                ).toResponse(),
                PersonalIdentifiableInformationSectionDto(
                    "Test Testsson",
                    "test@test.test",
                    PageImpl(listOf(session)).toResponse(),
                ),
                PageImpl(listOf(opinions)).toResponse(),
                PageImpl(listOf(outgoingAccessRequests)).toResponse(),
                PageImpl(listOf(incomingAccessRequests)).toResponse(),
                PageImpl(listOf(supportMessages)).toResponse(),
            )
        assertEquals(expected, actual)
    }
}