package com.r8n.backend.export

import com.r8n.backend.core.utils.TestObjectMapperConfiguration
import com.r8n.backend.core.utils.toResponse
import com.r8n.backend.export.api.ExportApi
import com.r8n.backend.export.api.dto.ExportStateDto
import com.r8n.backend.export.api.dto.ExportStatus
import com.r8n.backend.export.api.dto.UserCompleteDataDto
import com.r8n.backend.messaging.api.MessagingApi
import com.r8n.backend.messaging.api.dto.SupportThreadDto
import com.r8n.backend.messaging.api.dto.messaging.SupportMessageDto
import com.r8n.backend.messaging.api.dto.messaging.SupportParticipantRoleEnumDto
import com.r8n.backend.messaging.api.dto.messaging.SupportThreadSummaryDto
import com.r8n.backend.opinions.api.access.IncomingAccessRequestApi
import com.r8n.backend.opinions.api.access.OutgoingAccessRequestApi
import com.r8n.backend.opinions.api.access.dto.AccessRequestDto
import com.r8n.backend.opinions.api.access.dto.RequestStatusEnumDto
import com.r8n.backend.opinions.integration.api.OpinionListsInternalApi
import com.r8n.backend.opinions.stub.OpinionListTestDataFactory
import com.r8n.backend.security.ServiceTokenService
import com.r8n.backend.users.api.dto.UserStatusEnumDto
import com.r8n.backend.users.integration.api.UsersInternalApi
import com.r8n.backend.users.integration.api.dto.ConsentDto
import com.r8n.backend.users.integration.api.dto.PersonalIdentifiableInformationSectionDto
import com.r8n.backend.users.integration.api.dto.UserDto
import com.r8n.backend.users.integration.api.dto.UserSessionDto
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

@ActiveProfiles("test")
@AutoConfigureJsonTesters
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestObjectMapperConfiguration::class)
class ExportIntegrationTests {
    private companion object {
        object AccessRequestsTestDataFactory {
            fun get(
                listId: UUID? = null,
                status: RequestStatusEnumDto = RequestStatusEnumDto.SENT,
            ): AccessRequestDto {
                val listId = listId ?: UUID.randomUUID()
                return AccessRequestDto(
                    UUID.randomUUID(),
                    listId,
                    "the most complete rating of ${listId}s",
                    UUID.randomUUID(),
                    "world's leading expert in ${listId}s",
                    UUID.randomUUID(),
                    "world's biggest fan of ${listId}s",
                    Instant.now(),
                    status,
                )
            }
        }

        const val USER_ID = "00000000-0000-0000-0000-000000000000"
        val opinions = OpinionListTestDataFactory.getList()
        val incomingAccessRequests = AccessRequestsTestDataFactory.get()
        val outgoingAccessRequests = AccessRequestsTestDataFactory.get()
        val supportThreadId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val supportMessages =
            SupportThreadDto(
                supportThreadId,
                listOf("I have issue with post"),
            )
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var serviceTokenService: ServiceTokenService

    @MockitoBean
    lateinit var opinionClient: OpinionListsInternalApi

    @MockitoBean
    lateinit var incomingAccessRequestClient: IncomingAccessRequestApi

    @MockitoBean
    lateinit var outgoingAccessRequestClient: OutgoingAccessRequestApi

    @MockitoBean
    lateinit var messageClient: MessagingApi

    @MockitoBean
    lateinit var usersInternalApi: UsersInternalApi

    @BeforeEach
    fun setUp() {
        val timestamp = LocalDateTime.of(2024, 1, 1, 12, 0).toInstant(ZoneOffset.UTC)
        val session =
            UserSessionDto(
                UUID.fromString("01010101-0101-0101-0101-010101010101"),
                timestamp,
                timestamp.plus(1, ChronoUnit.DAYS),
                "127.0.0.1",
                "Unknown",
                "Test User Agent",
            )
        val userDto =
            UserDto(
                UUID.fromString(USER_ID),
                "Test Testsson",
                "test@test.test",
                UserStatusEnumDto.ACTIVE,
                timestamp,
                listOf(ConsentDto("PRIVACY_POLICY", timestamp, session)),
            )

        whenever(usersInternalApi.getUser(any())).thenReturn(userDto)
        whenever(usersInternalApi.getSessionsForUser(any(), anyOrNull())).thenReturn(
            PageImpl(listOf(session)).toResponse(),
        )
        whenever(opinionClient.getMineFull(any())).thenReturn(
            PageImpl(listOf(opinions)).toResponse(),
        )
        whenever(incomingAccessRequestClient.get(anyOrNull(), anyOrNull(), anyOrNull(), any())).thenReturn(
            PageImpl(listOf(incomingAccessRequests)).toResponse(),
        )
        whenever(outgoingAccessRequestClient.get(anyOrNull(), anyOrNull(), anyOrNull(), any())).thenReturn(
            PageImpl(listOf(outgoingAccessRequests)).toResponse(),
        )
        whenever(messageClient.getSupportThreadSummaries(any())).thenReturn(
            PageImpl(
                listOf(
                    SupportThreadSummaryDto(
                        id = supportThreadId,
                        ownerUserId = UUID.fromString(USER_ID),
                        createdAt = timestamp,
                        lastMessageAt = timestamp,
                    ),
                ),
            ).toResponse(),
        )
        whenever(messageClient.getSupportThreadMessages(any(), any())).thenReturn(
            PageImpl(
                listOf(
                    SupportMessageDto(
                        id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
                        threadId = supportThreadId,
                        authorUserId = UUID.fromString(USER_ID),
                        authorRole = SupportParticipantRoleEnumDto.USER,
                        text = "I have issue with post",
                        createdAt = timestamp,
                    ),
                ),
            ).toResponse(),
        )
    }

    @Test
    @WithMockUser(username = USER_ID)
    fun `export process follows three steps`() {
        val accessToken = serviceTokenService.generateAccessToken(UUID.fromString(USER_ID), listOf("USER"))

        // 1. Request export creation
        mockMvc
            .perform(
                post(ExportApi.START_PATH)
                    .header("Authorization", "Bearer $accessToken"),
            ).andExpect(status().isAccepted)

        // 2. Check status
        val statusResult =
            mockMvc
                .perform(
                    get(ExportApi.STATUS_PATH)
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val exportState: ExportStateDto = objectMapper.readValue(statusResult.response.contentAsString)
        assertEquals(ExportStatus.COMPLETED, exportState.status)

        // 3. Download data
        val downloadResult =
            mockMvc
                .perform(
                    get(ExportApi.DOWNLOAD_PATH)
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val actual: UserCompleteDataDto = objectMapper.readValue(downloadResult.response.contentAsString)

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
                "Unknown",
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
                    sessions = PageImpl(listOf(session)).toResponse(),
                ),
                PageImpl(listOf(opinions)).toResponse(),
                PageImpl(listOf(outgoingAccessRequests)).toResponse(),
                PageImpl(listOf(incomingAccessRequests)).toResponse(),
                PageImpl(listOf(supportMessages)).toResponse(),
            )
        assertEquals(expected, actual)
    }
}
