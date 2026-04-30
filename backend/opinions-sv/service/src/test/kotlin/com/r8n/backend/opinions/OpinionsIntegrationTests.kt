package com.r8n.backend.opinions

import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.api.opinions.dto.ModerationDecisionActionDto
import com.r8n.backend.opinions.api.opinions.dto.ModerationDecisionDto
import com.r8n.backend.opinions.api.opinions.dto.OpinionDto
import com.r8n.backend.opinions.api.opinions.dto.OpinionStatusEnumDto
import com.r8n.backend.opinions.api.opinions.dto.OpinionSubjectDto
import com.r8n.backend.opinions.api.opinions.dto.ReferentDto
import com.r8n.backend.opinions.api.opinions.dto.RejectOpinionRequestDto
import com.r8n.backend.opinions.api.referents.dto.CreateReferentRequestDto
import com.r8n.backend.opinions.api.subjects.dto.CreateSubjectRequestDto
import com.r8n.backend.opinions.api.subjects.dto.SUBJECT_NAME_MAX_LENGTH
import com.r8n.backend.opinions.stub.OpinionSubjectTestDataFactory.bernardReferent
import com.r8n.backend.opinions.stub.OpinionSubjectTestDataFactory.cappuccino1A
import com.r8n.backend.opinions.stub.OpinionSubjectTestDataFactory.cappuccino1G
import com.r8n.backend.security.ServiceTokenService
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
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
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
        val STRANGER: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val REQUESTER: UUID = UUID.fromString("09090909-0909-0909-0909-090909090909")
        val OWNER: UUID = UUID.fromString("10101010-1010-1010-1010-101010101010")

        @Suppress("unused") // used to store test database container
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

    @Autowired
    lateinit var serviceTokenService: ServiceTokenService

    @BeforeEach
    fun setUp() {
        whenever(usersInternalApi.getUserName(eq(OWNER)))
            .thenReturn(bernardReferent.name)
        whenever(usersInternalApi.getUserName(eq(REQUESTER)))
            .thenReturn(bernardReferent.name)
        whenever(usersInternalApi.getUserName(eq(STRANGER)))
            .thenReturn("Moderator User")
    }

    @Test
    @WithMockUser
    fun `find subject works`() {
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val result =
            mockMvc
                .perform(
                    get("/api/subjects/find")
                        .with(csrf())
                        .queryParam("query", "CAPPUCCINO")
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val actual: PageResponseDto<OpinionSubjectDto> = objectMapper.readValue(result.response.contentAsString)

        assertEquals(0, actual.page)
        assertEquals(10, actual.size)
        assertEquals(
            listOf(cappuccino1A.id, cappuccino1G.id),
            actual.items.map { it.id },
        )
        assertEquals(cappuccino1A.name, actual.items.first().name)
    }

    @Test
    @WithMockUser
    fun `create subject works`() {
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val request =
            CreateSubjectRequestDto(
                name = "Subject API Test Espresso",
                referentName = "Subject API Test Espresso at Test Cafe",
                address = "Berlin, Teststraße 27",
                latitude = 52.5,
                longitude = 13.4,
            )
        val createResult =
            mockMvc
                .perform(
                    post("/api/subjects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val created: OpinionSubjectDto = objectMapper.readValue(createResult.response.contentAsString)
        assertEquals(request.name, created.name)
        assertEquals(request.referentName, created.primaryReferent?.name)
        assertEquals(request.address, created.primaryReferent?.address)
        assertEquals(request.latitude, created.primaryReferent?.latitude)
        assertEquals(request.longitude, created.primaryReferent?.longitude)
        assertEquals(0, created.alternativeReferents.size)

        val findResult =
            mockMvc
                .perform(
                    get("/api/subjects/find")
                        .with(csrf())
                        .queryParam("query", request.name)
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val found: PageResponseDto<OpinionSubjectDto> = objectMapper.readValue(findResult.response.contentAsString)
        assertEquals(listOf(created.id), found.items.map { it.id })
    }

    @Test
    @WithMockUser
    fun `create and find referent works`() {
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val request =
            CreateReferentRequestDto(
                name = "Berlin 42",
                address = "Berlin, Teststraße 42",
                latitude = 52.51,
                longitude = 13.37,
            )

        val createResult =
            mockMvc
                .perform(
                    post("/api/referents")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val created: ReferentDto = objectMapper.readValue(createResult.response.contentAsString)
        assertEquals(request.name, created.name)
        assertEquals(request.address, created.address)
        assertEquals(request.latitude, created.latitude)
        assertEquals(request.longitude, created.longitude)

        val findResult =
            mockMvc
                .perform(
                    get("/api/referents/find")
                        .with(csrf())
                        .queryParam("query", request.name)
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .queryParam("sort[0].property", "name")
                        .queryParam("sort[0].direction", "ASC")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val found: PageResponseDto<ReferentDto> = objectMapper.readValue(findResult.response.contentAsString)
        assertEquals(listOf(created.id), found.items.map { it.id })
    }

    @Test
    @WithMockUser
    fun `create subject can reuse primary referent id`() {
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val referentRequest =
            CreateReferentRequestDto(
                name = "Coda Dessert Bar",
                address = "Berlin, Teststraße 99",
                latitude = 52.49,
                longitude = 13.41,
            )
        val referentResult =
            mockMvc
                .perform(
                    post("/api/referents")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(referentRequest))
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val referent: ReferentDto = objectMapper.readValue(referentResult.response.contentAsString)

        val subjectRequest =
            CreateSubjectRequestDto(
                name = "Berlin 42 chairs",
                primaryReferentId = referent.id,
                referentName = null,
                address = null,
                latitude = null,
                longitude = null,
            )
        val subjectResult =
            mockMvc
                .perform(
                    post("/api/subjects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subjectRequest))
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val created: OpinionSubjectDto = objectMapper.readValue(subjectResult.response.contentAsString)
        assertEquals(subjectRequest.name, created.name)
        assertEquals(referent.id, created.primaryReferent?.id)
        assertEquals(referent.name, created.primaryReferent?.name)
    }

    @Test
    @WithMockUser
    fun `set primary referent updates subject`() {
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val subjectRequest =
            CreateSubjectRequestDto(
                name = "Subject referent update test",
                referentName = "Initial Place",
                address = null,
                latitude = null,
                longitude = null,
            )
        val subjectResult =
            mockMvc
                .perform(
                    post("/api/subjects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subjectRequest))
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val subject: OpinionSubjectDto = objectMapper.readValue(subjectResult.response.contentAsString)

        val referentRequest =
            CreateReferentRequestDto(
                name = "Updated Place",
                address = "Berlin, Updated street 1",
                latitude = 52.52,
                longitude = 13.40,
            )
        val referentResult =
            mockMvc
                .perform(
                    post("/api/referents")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(referentRequest))
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val referent: ReferentDto = objectMapper.readValue(referentResult.response.contentAsString)

        val patchResult =
            mockMvc
                .perform(
                    patch("/api/subjects/${subject.id}/set-primary-referent")
                        .with(csrf())
                        .queryParam("referentId", referent.id.toString())
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val updated: OpinionSubjectDto = objectMapper.readValue(patchResult.response.contentAsString)
        assertEquals(subject.id, updated.id)
        assertEquals(referent.id, updated.primaryReferent?.id)
        assertEquals(referent.name, updated.primaryReferent?.name)
        assertEquals(referent.address, updated.primaryReferent?.address)
    }

    @Test
    @WithMockUser
    fun `creating subject with oversized name returns bad request`() {
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val request =
            CreateSubjectRequestDto(
                name = "a".repeat(SUBJECT_NAME_MAX_LENGTH + 1),
                referentName = "Valid referent name",
                address = null,
                latitude = null,
                longitude = null,
            )

        mockMvc
            .perform(
                post("/api/subjects")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .header("Authorization", "Bearer $accessToken"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser
    fun `creating subject with oversized referent name returns bad request`() {
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val request =
            CreateSubjectRequestDto(
                name = "Valid subject name",
                referentName = "a".repeat(SUBJECT_NAME_MAX_LENGTH + 1),
                address = null,
                latitude = null,
                longitude = null,
            )

        mockMvc
            .perform(
                post("/api/subjects")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .header("Authorization", "Bearer $accessToken"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser
    fun `get opinion works`() {
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val requestedId = "30000000-0000-0000-0000-000000000001"
        val result =
            mockMvc
                .perform(
                    get("/api/opinions/$requestedId")
                        .with(csrf())
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

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

    @Test
    @WithMockUser
    fun `get opinion for subject works`() {
        val requestedSubjectId = "14141414-1414-1414-1414-141414141414"
        val accessToken = serviceTokenService.generateAccessToken(OWNER, listOf("USER"))
        val result =
            mockMvc
                .perform(
                    get("/api/opinions/for/$requestedSubjectId")
                        .with(csrf())
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

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

    @Test
    @WithMockUser
    fun `create opinion works`() {
        val subjectId = "15151515-1515-1515-1515-151515151515"
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val result =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", subjectId)
                        .queryParam("subjective", "new subjective")
                        .queryParam("objective", "new objective")
                        .queryParam("mark", "4.50")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val actual: OpinionDto = objectMapper.readValue(result.response.contentAsString)
        assertEquals(REQUESTER, actual.owner)
        assertEquals(bernardReferent.name, actual.ownerName)
        assertEquals(UUID.fromString(subjectId), actual.subject)
        assertEquals(cappuccino1G.name, actual.subjectName)
        assertEquals(listOf("new subjective"), actual.subjective)
        assertEquals(listOf("new objective"), actual.objective)
        assertEquals(4.5, actual.mark)
        assertEquals(OpinionStatusEnumDto.DRAFT, actual.status)
    }

    @Test
    @WithMockUser
    fun `submit opinion for moderation moves owner draft to pending premoderation`() {
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val createResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "pending subjective")
                        .queryParam("objective", "pending objective")
                        .queryParam("mark", "6.30")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val created: OpinionDto = objectMapper.readValue(createResult.response.contentAsString)

        val submitResult =
            mockMvc
                .perform(
                    post("/api/opinions/${created.id}/submit-for-moderation")
                        .with(csrf())
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val actual: OpinionDto = objectMapper.readValue(submitResult.response.contentAsString)
        assertEquals(created.id, actual.id)
        assertEquals(REQUESTER, actual.owner)
        assertEquals(OpinionStatusEnumDto.PENDING_PREMODERATION, actual.status)
    }

    @Test
    @WithMockUser
    fun `submit opinion for moderation rejects non owner`() {
        val ownerToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val otherUserToken = serviceTokenService.generateAccessToken(STRANGER, listOf("USER"))
        val createResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "owner only subjective")
                        .queryParam("objective", "owner only objective")
                        .queryParam("mark", "7.20")
                        .header("Authorization", "Bearer $ownerToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val created: OpinionDto = objectMapper.readValue(createResult.response.contentAsString)

        mockMvc
            .perform(
                post("/api/opinions/${created.id}/submit-for-moderation")
                    .with(csrf())
                    .header("Authorization", "Bearer $otherUserToken"),
            ).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser
    fun `submit opinion for moderation rejects non draft opinion`() {
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val createResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "single submit subjective")
                        .queryParam("objective", "single submit objective")
                        .queryParam("mark", "8.10")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val created: OpinionDto = objectMapper.readValue(createResult.response.contentAsString)

        mockMvc
            .perform(
                post("/api/opinions/${created.id}/submit-for-moderation")
                    .with(csrf())
                    .header("Authorization", "Bearer $accessToken"),
            ).andExpect(status().isOk)

        mockMvc
            .perform(
                post("/api/opinions/${created.id}/submit-for-moderation")
                    .with(csrf())
                    .header("Authorization", "Bearer $accessToken"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser
    fun `moderator can list pending moderation opinions`() {
        val ownerToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val moderatorToken = serviceTokenService.generateAccessToken(STRANGER, listOf("MODERATOR"))
        val created = createDraftOpinion(ownerToken, "moderation queue subjective")
        submitForModeration(ownerToken, created.id)

        val result =
            mockMvc
                .perform(
                    get("/api/opinions/moderation")
                        .with(csrf())
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .header("Authorization", "Bearer $moderatorToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val actual: PageResponseDto<OpinionDto> = objectMapper.readValue(result.response.contentAsString)
        assertEquals(true, actual.items.any { it.id == created.id })
        assertEquals(true, actual.items.all { it.status == OpinionStatusEnumDto.PENDING_PREMODERATION })
    }

    @Test
    @WithMockUser
    fun `regular user cannot list pending moderation opinions`() {
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))

        mockMvc
            .perform(
                get("/api/opinions/moderation")
                    .with(csrf())
                    .queryParam("page", "0")
                    .queryParam("size", "20")
                    .header("Authorization", "Bearer $accessToken"),
            ).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser
    fun `moderator can approve pending opinion`() {
        val ownerToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val moderatorToken = serviceTokenService.generateAccessToken(STRANGER, listOf("MODERATOR"))
        val created = createDraftOpinion(ownerToken, "approval subjective")
        submitForModeration(ownerToken, created.id)

        val approveResult =
            mockMvc
                .perform(
                    post("/api/opinions/${created.id}/approve")
                        .with(csrf())
                        .header("Authorization", "Bearer $moderatorToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val actual: OpinionDto = objectMapper.readValue(approveResult.response.contentAsString)
        assertEquals(created.id, actual.id)
        assertEquals(OpinionStatusEnumDto.PUBLISHED, actual.status)

        val decisionsResult =
            mockMvc
                .perform(
                    get("/api/opinions/moderation/decisions")
                        .with(csrf())
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .header("Authorization", "Bearer $moderatorToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val decisions: PageResponseDto<ModerationDecisionDto> =
            objectMapper.readValue(decisionsResult.response.contentAsString)
        val decision = decisions.items.first { it.opinionId == created.id }
        assertEquals(ModerationDecisionActionDto.APPROVED, decision.action)
        assertEquals(OpinionStatusEnumDto.PENDING_PREMODERATION, decision.previousStatus)
        assertEquals(OpinionStatusEnumDto.PUBLISHED, decision.newStatus)
        assertEquals(STRANGER, decision.moderatorId)
        assertEquals("Moderator User", decision.moderatorName)
        assertEquals(null, decision.reason)
    }

    @Test
    @WithMockUser
    fun `moderator can reject pending opinion with reason`() {
        val ownerToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val moderatorToken = serviceTokenService.generateAccessToken(STRANGER, listOf("MODERATOR"))
        val created = createDraftOpinion(ownerToken, "rejection subjective")
        submitForModeration(ownerToken, created.id)

        val rejectResult =
            mockMvc
                .perform(
                    post("/api/opinions/${created.id}/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                RejectOpinionRequestDto("Please remove unsupported personal claims."),
                            ),
                        ).header("Authorization", "Bearer $moderatorToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val actual: OpinionDto = objectMapper.readValue(rejectResult.response.contentAsString)
        assertEquals(created.id, actual.id)
        assertEquals(OpinionStatusEnumDto.REJECTED, actual.status)

        val decisionsResult =
            mockMvc
                .perform(
                    get("/api/opinions/moderation/decisions")
                        .with(csrf())
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .header("Authorization", "Bearer $moderatorToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val decisions: PageResponseDto<ModerationDecisionDto> =
            objectMapper.readValue(decisionsResult.response.contentAsString)
        val decision = decisions.items.first { it.opinionId == created.id }
        assertEquals(ModerationDecisionActionDto.REJECTED, decision.action)
        assertEquals(OpinionStatusEnumDto.PENDING_PREMODERATION, decision.previousStatus)
        assertEquals(OpinionStatusEnumDto.REJECTED, decision.newStatus)
        assertEquals("Please remove unsupported personal claims.", decision.reason)
    }

    @Test
    @WithMockUser
    fun `regular user cannot list moderation decisions`() {
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))

        mockMvc
            .perform(
                get("/api/opinions/moderation/decisions")
                    .with(csrf())
                    .queryParam("page", "0")
                    .queryParam("size", "20")
                    .header("Authorization", "Bearer $accessToken"),
            ).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser
    fun `reject opinion requires non blank reason`() {
        val ownerToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val moderatorToken = serviceTokenService.generateAccessToken(STRANGER, listOf("MODERATOR"))
        val created = createDraftOpinion(ownerToken, "blank rejection subjective")
        submitForModeration(ownerToken, created.id)

        mockMvc
            .perform(
                post("/api/opinions/${created.id}/reject")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(RejectOpinionRequestDto(" ")))
                    .header("Authorization", "Bearer $moderatorToken"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser
    fun `moderator cannot approve draft opinion`() {
        val ownerToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val moderatorToken = serviceTokenService.generateAccessToken(STRANGER, listOf("MODERATOR"))
        val created = createDraftOpinion(ownerToken, "draft approval subjective")

        mockMvc
            .perform(
                post("/api/opinions/${created.id}/approve")
                    .with(csrf())
                    .header("Authorization", "Bearer $moderatorToken"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser
    fun `update opinion works`() {
        val accessToken = serviceTokenService.generateAccessToken(OWNER, listOf("USER"))
        val createResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "to be replaced subjective")
                        .queryParam("objective", "to be replaced objective")
                        .queryParam("mark", "2.10")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val created: OpinionDto = objectMapper.readValue(createResult.response.contentAsString)

        val updateResult =
            mockMvc
                .perform(
                    patch("/api/opinions/${created.id}")
                        .with(csrf())
                        .queryParam("subjective", "updated subjective")
                        .queryParam("objective", "updated objective")
                        .queryParam("mark", "4.90")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val actual: OpinionDto = objectMapper.readValue(updateResult.response.contentAsString)
        assertEquals(created.id, actual.id)
        assertEquals(OWNER, actual.owner)
        assertEquals(bernardReferent.name, actual.ownerName)
        assertEquals(UUID.fromString("15151515-1515-1515-1515-151515151515"), actual.subject)
        assertEquals(cappuccino1G.name, actual.subjectName)
        assertEquals(listOf("updated subjective"), actual.subjective)
        assertEquals(listOf("updated objective"), actual.objective)
        assertEquals(4.9, actual.mark)
        assertEquals(OpinionStatusEnumDto.DRAFT, actual.status)
    }

    @Test
    @WithMockUser
    fun `delete opinion works`() {
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val createResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "to be removed subjective")
                        .queryParam("objective", "to be removed objective")
                        .queryParam("mark", "1.10")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val created: OpinionDto = objectMapper.readValue(createResult.response.contentAsString)

        mockMvc
            .perform(
                delete("/api/opinions/${created.id}")
                    .with(csrf())
                    .header("Authorization", "Bearer $accessToken"),
            ).andExpect(status().isOk)

        mockMvc
            .perform(
                get("/api/opinions/${created.id}")
                    .header("Authorization", "Bearer $accessToken"),
            ).andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser
    fun `link component works`() {
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))

        val parentCreateResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "parent subjective")
                        .queryParam("objective", "parent objective")
                        .queryParam("mark", "2.00")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val parent: OpinionDto = objectMapper.readValue(parentCreateResult.response.contentAsString)

        val childCreateResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "child subjective")
                        .queryParam("objective", "child objective")
                        .queryParam("mark", "4.00")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val child: OpinionDto = objectMapper.readValue(childCreateResult.response.contentAsString)

        val linkResult =
            mockMvc
                .perform(
                    post("/api/opinions/link")
                        .with(csrf())
                        .queryParam("parentOpinionId", parent.id.toString())
                        .queryParam("childOpinionId", child.id.toString())
                        .queryParam("weight", "0.25")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val actual: OpinionDto = objectMapper.readValue(linkResult.response.contentAsString)
        assertEquals(parent.id, actual.id)
        assertEquals(1, actual.components.size)
        assertEquals(child.id, actual.components.first().opinion)
        assertEquals(0.25, actual.components.first().weight)
        assertEquals(1.0, actual.componentMark)
    }

    @Test
    @WithMockUser
    fun `link component is idempotent for duplicate request`() {
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))

        val parentCreateResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "parent duplicate subjective")
                        .queryParam("objective", "parent duplicate objective")
                        .queryParam("mark", "2.00")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val parent: OpinionDto = objectMapper.readValue(parentCreateResult.response.contentAsString)

        val childCreateResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "child duplicate subjective")
                        .queryParam("objective", "child duplicate objective")
                        .queryParam("mark", "4.00")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val child: OpinionDto = objectMapper.readValue(childCreateResult.response.contentAsString)

        mockMvc
            .perform(
                post("/api/opinions/link")
                    .with(csrf())
                    .queryParam("parentOpinionId", parent.id.toString())
                    .queryParam("childOpinionId", child.id.toString())
                    .queryParam("weight", "0.25")
                    .header("Authorization", "Bearer $accessToken"),
            ).andExpect(status().isOk)

        val secondLinkResult =
            mockMvc
                .perform(
                    post("/api/opinions/link")
                        .with(csrf())
                        .queryParam("parentOpinionId", parent.id.toString())
                        .queryParam("childOpinionId", child.id.toString())
                        .queryParam("weight", "0.25")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val actual: OpinionDto = objectMapper.readValue(secondLinkResult.response.contentAsString)
        assertEquals(1, actual.components.size)
        assertEquals(child.id, actual.components.first().opinion)
        assertEquals(0.25, actual.components.first().weight)
        assertEquals(1.0, actual.componentMark)
    }

    @Test
    @WithMockUser
    fun `link component forbidden for non owner`() {
        val ownerToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val otherUserToken = serviceTokenService.generateAccessToken(UUID.randomUUID(), listOf("USER"))

        val parentCreateResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "parent owner subjective")
                        .queryParam("objective", "parent owner objective")
                        .queryParam("mark", "2.00")
                        .header("Authorization", "Bearer $ownerToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val parent: OpinionDto = objectMapper.readValue(parentCreateResult.response.contentAsString)

        mockMvc
            .perform(
                post("/api/opinions/link")
                    .with(csrf())
                    .queryParam("parentOpinionId", parent.id.toString())
                    .queryParam("childOpinionId", "30000000-0000-0000-0000-000000000001")
                    .queryParam("weight", "0.25")
                    .header("Authorization", "Bearer $otherUserToken"),
            ).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser
    fun `unlink component works`() {
        val accessToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))

        val parentCreateResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "parent unlink subjective")
                        .queryParam("objective", "parent unlink objective")
                        .queryParam("mark", "2.00")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val parent: OpinionDto = objectMapper.readValue(parentCreateResult.response.contentAsString)

        val childCreateResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "child unlink subjective")
                        .queryParam("objective", "child unlink objective")
                        .queryParam("mark", "4.00")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val child: OpinionDto = objectMapper.readValue(childCreateResult.response.contentAsString)

        val linkResult =
            mockMvc
                .perform(
                    post("/api/opinions/link")
                        .with(csrf())
                        .queryParam("parentOpinionId", parent.id.toString())
                        .queryParam("childOpinionId", child.id.toString())
                        .queryParam("weight", "0.25")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val linked: OpinionDto = objectMapper.readValue(linkResult.response.contentAsString)
        val linkId = linked.components.first().id

        val unlinkResult =
            mockMvc
                .perform(
                    delete("/api/opinions/unlink/$linkId")
                        .with(csrf())
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val actual: OpinionDto = objectMapper.readValue(unlinkResult.response.contentAsString)

        assertEquals(parent.id, actual.id)
        assertEquals(0, actual.components.size)
        assertEquals(null, actual.componentMark)
    }

    @Test
    @WithMockUser
    fun `unlink component forbidden for non owner`() {
        val ownerToken = serviceTokenService.generateAccessToken(REQUESTER, listOf("USER"))
        val otherUserToken = serviceTokenService.generateAccessToken(UUID.randomUUID(), listOf("USER"))

        val parentCreateResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "parent unlink owner subjective")
                        .queryParam("objective", "parent unlink owner objective")
                        .queryParam("mark", "2.00")
                        .header("Authorization", "Bearer $ownerToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val parent: OpinionDto = objectMapper.readValue(parentCreateResult.response.contentAsString)

        val linkResult =
            mockMvc
                .perform(
                    post("/api/opinions/link")
                        .with(csrf())
                        .queryParam("parentOpinionId", parent.id.toString())
                        .queryParam("childOpinionId", "30000000-0000-0000-0000-000000000001")
                        .queryParam("weight", "0.25")
                        .header("Authorization", "Bearer $ownerToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val linked: OpinionDto = objectMapper.readValue(linkResult.response.contentAsString)
        val linkId = linked.components.first().id

        mockMvc
            .perform(
                delete("/api/opinions/unlink/$linkId")
                    .with(csrf())
                    .header("Authorization", "Bearer $otherUserToken"),
            ).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser
    fun `adjust component weight works`() {
        val accessToken = serviceTokenService.generateAccessToken(OWNER, listOf("USER"))

        val parentCreateResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "parent adjust subjective")
                        .queryParam("objective", "parent adjust objective")
                        .queryParam("mark", "2.00")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val parent: OpinionDto = objectMapper.readValue(parentCreateResult.response.contentAsString)

        val childCreateResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "child adjust subjective")
                        .queryParam("objective", "child adjust objective")
                        .queryParam("mark", "4.00")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val child: OpinionDto = objectMapper.readValue(childCreateResult.response.contentAsString)

        val linkResult =
            mockMvc
                .perform(
                    post("/api/opinions/link")
                        .with(csrf())
                        .queryParam("parentOpinionId", parent.id.toString())
                        .queryParam("childOpinionId", child.id.toString())
                        .queryParam("weight", "0.25")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val linked: OpinionDto = objectMapper.readValue(linkResult.response.contentAsString)
        val linkId = linked.components.first().id

        val adjustResult =
            mockMvc
                .perform(
                    patch("/api/opinions/adjust-weight/$linkId")
                        .with(csrf())
                        .queryParam("weight", "0.50")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val actual: OpinionDto = objectMapper.readValue(adjustResult.response.contentAsString)

        assertEquals(parent.id, actual.id)
        assertEquals(1, actual.components.size)
        assertEquals(linkId, actual.components.first().id)
        assertEquals(child.id, actual.components.first().opinion)
        assertEquals(0.5, actual.components.first().weight)
        assertEquals(2.0, actual.componentMark)
    }

    @Test
    @WithMockUser
    fun `adjust component weight forbidden for non owner`() {
        val ownerToken = serviceTokenService.generateAccessToken(OWNER, listOf("USER"))
        val otherUserToken = serviceTokenService.generateAccessToken(STRANGER, listOf("USER"))

        val parentCreateResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "parent adjust owner subjective")
                        .queryParam("objective", "parent adjust owner objective")
                        .queryParam("mark", "2.00")
                        .header("Authorization", "Bearer $ownerToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val parent: OpinionDto = objectMapper.readValue(parentCreateResult.response.contentAsString)

        val childCreateResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", "child adjust owner subjective")
                        .queryParam("objective", "child adjust owner objective")
                        .queryParam("mark", "4.00")
                        .header("Authorization", "Bearer $ownerToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val child: OpinionDto = objectMapper.readValue(childCreateResult.response.contentAsString)

        val linkResult =
            mockMvc
                .perform(
                    post("/api/opinions/link")
                        .with(csrf())
                        .queryParam("parentOpinionId", parent.id.toString())
                        .queryParam("childOpinionId", child.id.toString())
                        .queryParam("weight", "0.25")
                        .header("Authorization", "Bearer $ownerToken"),
                ).andExpect(status().isOk)
                .andReturn()
        val linked: OpinionDto = objectMapper.readValue(linkResult.response.contentAsString)
        val linkId = linked.components.first().id

        mockMvc
            .perform(
                patch("/api/opinions/adjust-weight/$linkId")
                    .with(csrf())
                    .queryParam("weight", "0.50")
                    .header("Authorization", "Bearer $otherUserToken"),
            ).andExpect(status().isForbidden)
    }

    private fun createDraftOpinion(
        accessToken: String?,
        subjective: String,
    ): OpinionDto {
        val createResult =
            mockMvc
                .perform(
                    post("/api/opinions")
                        .with(csrf())
                        .queryParam("subjectId", "15151515-1515-1515-1515-151515151515")
                        .queryParam("subjective", subjective)
                        .queryParam("objective", "moderation objective")
                        .queryParam("mark", "5.00")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        return objectMapper.readValue(createResult.response.contentAsString)
    }

    private fun submitForModeration(
        accessToken: String?,
        opinionId: UUID,
    ) {
        mockMvc
            .perform(
                post("/api/opinions/$opinionId/submit-for-moderation")
                    .with(csrf())
                    .header("Authorization", "Bearer $accessToken"),
            ).andExpect(status().isOk)
    }
}
