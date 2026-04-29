package com.r8n.backend.opinions.lists

import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.TestObjectMapperConfiguration
import com.r8n.backend.opinions.api.lists.dto.OpinionListSummaryDto
import com.r8n.backend.opinions.lists.database.OpinionListRepository
import com.r8n.backend.opinions.lists.database.OpinionsToOpinionListsRepository
import com.r8n.backend.opinions.lists.domain.OpinionListPrivacyEnum
import com.r8n.backend.opinions.lists.persistence.OpinionListPersistence
import com.r8n.backend.opinions.lists.persistence.OpinionsToOpinionListsPersistence
import com.r8n.backend.opinions.opinions.database.OpinionRepository
import com.r8n.backend.opinions.opinions.database.OpinionSubjectRepository
import com.r8n.backend.opinions.opinions.database.ReferentRepository
import com.r8n.backend.opinions.opinions.domain.OpinionStatusEnum
import com.r8n.backend.opinions.opinions.persistence.OpinionPersistence
import com.r8n.backend.opinions.opinions.persistence.OpinionSubjectPersistence
import com.r8n.backend.opinions.opinions.persistence.ReferentPersistence
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
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@ActiveProfiles("test")
@Testcontainers
@AutoConfigureJsonTesters
@AutoConfigureMockMvc
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@Import(TestObjectMapperConfiguration::class)
class OpinionListNewFiltersIntegrationTest {
    private companion object {
        val USER_ID: UUID = UUID.fromString("40404040-4040-4040-4040-404040404040")

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

    @Autowired
    lateinit var referentRepository: ReferentRepository

    @Autowired
    lateinit var subjectRepository: OpinionSubjectRepository

    @Autowired
    lateinit var opinionRepository: OpinionRepository

    @Autowired
    lateinit var opinionListRepository: OpinionListRepository

    @Autowired
    lateinit var opinionsAssignmentRepository: OpinionsToOpinionListsRepository

    @MockitoBean
    lateinit var usersInternalApi: UsersInternalApi

    lateinit var userToken: String

    @BeforeEach
    fun setUp() {
        userToken = "Bearer " + serviceTokenService.generateAccessToken(USER_ID, listOf("USER"))
        whenever(usersInternalApi.isAnyModerator(any())).thenReturn(false)
        whenever(usersInternalApi.getUserName(any())).thenReturn("Test User")
    }

    @Test
    fun `search by location works`() {
        // Setup: Referent -> Subject -> Opinion -> OpinionList
        val referent =
            referentRepository.save(
                ReferentPersistence(
                    name = "Cafe Berlin",
                    address = "Friedrichstrasse 123, Berlin",
                    latitude = null,
                    longitude = null,
                    referentGroup = UUID.randomUUID(),
                ),
            )
        val subject =
            subjectRepository.save(
                OpinionSubjectPersistence(
                    name = "Berlin Cafe Subject",
                    referent = referent.id!!,
                ),
            )
        val opinion =
            opinionRepository.save(
                OpinionPersistence(
                    owner = USER_ID,
                    subject = subject.id!!,
                    mark = 5.0,
                    status = OpinionStatusEnum.PUBLISHED,
                    timestamp = Instant.now(),
                ),
            )
        val list =
            opinionListRepository.save(
                OpinionListPersistence(
                    owner = USER_ID,
                    name = "Berlin Lists",
                    privacy = OpinionListPrivacyEnum.SEARCHABLE,
                ),
            )
        opinionsAssignmentRepository.save(
            OpinionsToOpinionListsPersistence(
                opinionList = list.id!!,
                opinion = opinion.id!!,
                weight = 1.0,
            ),
        )

        val result =
            mockMvc
                .perform(
                    get("/api/opinion-lists/search")
                        .header("Authorization", userToken)
                        .param("locationFilter.containsLocationSubstring", "Berlin")
                        .param("page", "0")
                        .param("size", "10"),
                ).andExpect(status().isOk)
                .andReturn()

        val page = objectMapper.readValue<PageResponseDto<OpinionListSummaryDto>>(result.response.contentAsString)
        assertThat(page.items.map { it.listName }).contains("Berlin Lists")
    }

    @Test
    fun `search by subject works`() {
        val referent =
            referentRepository.save(
                ReferentPersistence(
                    name = "R1",
                    address = "A1",
                    latitude = null,
                    longitude = null,
                    referentGroup = UUID.randomUUID(),
                ),
            )
        val subject =
            subjectRepository.save(
                OpinionSubjectPersistence(name = "Special Sushi", referent = referent.id!!),
            )
        val opinion =
            opinionRepository.save(
                OpinionPersistence(
                    owner = USER_ID,
                    subject = subject.id!!,
                    status = OpinionStatusEnum.PUBLISHED,
                    timestamp = Instant.now(),
                    mark = null,
                ),
            )
        val list =
            opinionListRepository.save(
                OpinionListPersistence(
                    owner = USER_ID,
                    name = "Sushi List",
                    privacy = OpinionListPrivacyEnum.SEARCHABLE,
                ),
            )
        opinionsAssignmentRepository.save(
            OpinionsToOpinionListsPersistence(opinionList = list.id!!, opinion = opinion.id!!, weight = 1.0),
        )

        val result =
            mockMvc
                .perform(
                    get("/api/opinion-lists/search")
                        .header("Authorization", userToken)
                        .param("containsSubjectSubstring", "Sushi")
                        .param("page", "0")
                        .param("size", "10"),
                ).andExpect(status().isOk)
                .andReturn()

        val page = objectMapper.readValue<PageResponseDto<OpinionListSummaryDto>>(result.response.contentAsString)
        assertThat(page.items.map { it.listName }).contains("Sushi List")
    }

    @Test
    fun `search by age works`() {
        val referent =
            referentRepository.save(
                ReferentPersistence(
                    name = "R2",
                    address = "A2",
                    latitude = null,
                    longitude = null,
                    referentGroup = UUID.randomUUID(),
                ),
            )
        val subject = subjectRepository.save(OpinionSubjectPersistence(name = "S2", referent = referent.id!!))

        val oldOpinion =
            opinionRepository.save(
                OpinionPersistence(
                    owner = USER_ID,
                    subject = subject.id!!,
                    status = OpinionStatusEnum.PUBLISHED,
                    timestamp = Instant.now().minus(10, ChronoUnit.DAYS),
                    mark = null,
                ),
            )
        val newOpinion =
            opinionRepository.save(
                OpinionPersistence(
                    owner = USER_ID,
                    subject = subject.id!!,
                    status = OpinionStatusEnum.PUBLISHED,
                    timestamp = Instant.now().minus(1, ChronoUnit.DAYS),
                    mark = null,
                ),
            )

        val oldList =
            opinionListRepository.save(
                OpinionListPersistence(owner = USER_ID, name = "Old List", privacy = OpinionListPrivacyEnum.SEARCHABLE),
            )
        val newList =
            opinionListRepository.save(
                OpinionListPersistence(owner = USER_ID, name = "New List", privacy = OpinionListPrivacyEnum.SEARCHABLE),
            )

        opinionsAssignmentRepository.save(
            OpinionsToOpinionListsPersistence(opinionList = oldList.id!!, opinion = oldOpinion.id!!, weight = 1.0),
        )
        opinionsAssignmentRepository.save(
            OpinionsToOpinionListsPersistence(opinionList = newList.id!!, opinion = newOpinion.id!!, weight = 1.0),
        )

        val result =
            mockMvc
                .perform(
                    get("/api/opinion-lists/search")
                        .header("Authorization", userToken)
                        .param("someOpinionsYoungerThan", Instant.now().minus(5, ChronoUnit.DAYS).toString())
                        .param("page", "0")
                        .param("size", "10"),
                ).andExpect(status().isOk)
                .andReturn()

        val page = objectMapper.readValue<PageResponseDto<OpinionListSummaryDto>>(result.response.contentAsString)
        assertThat(page.items.map { it.listName }).contains("New List")
        assertThat(page.items.map { it.listName }).doesNotContain("Old List")
    }

    @Test
    fun `findThisTextInAnyOfTheAbove works`() {
        // 1. Matches list name
        opinionListRepository.save(
            OpinionListPersistence(owner = USER_ID, name = "FindMe List", privacy = OpinionListPrivacyEnum.SEARCHABLE),
        )

        // 2. Matches subject name
        val referent =
            referentRepository.save(
                ReferentPersistence(
                    name = "R3",
                    address = "A3",
                    latitude = null,
                    longitude = null,
                    referentGroup = UUID.randomUUID(),
                ),
            )
        val subject =
            subjectRepository.save(
                OpinionSubjectPersistence(name = "FindMe Subject", referent = referent.id!!),
            )
        val opinionS =
            opinionRepository.save(
                OpinionPersistence(
                    owner = USER_ID,
                    subject = subject.id!!,
                    status = OpinionStatusEnum.PUBLISHED,
                    timestamp = Instant.now(),
                    mark = null,
                ),
            )
        val listS =
            opinionListRepository.save(
                OpinionListPersistence(owner = USER_ID, name = "S List", privacy = OpinionListPrivacyEnum.SEARCHABLE),
            )
        opinionsAssignmentRepository.save(
            OpinionsToOpinionListsPersistence(opinionList = listS.id!!, opinion = opinionS.id!!, weight = 1.0),
        )

        // 3. Matches address
        val referentA =
            referentRepository.save(
                ReferentPersistence(
                    name = "R4",
                    address = "FindMe Street",
                    latitude = null,
                    longitude = null,
                    referentGroup = UUID.randomUUID(),
                ),
            )
        val subjectA = subjectRepository.save(OpinionSubjectPersistence(name = "S4", referent = referentA.id!!))
        val opinionA =
            opinionRepository.save(
                OpinionPersistence(
                    owner = USER_ID,
                    subject = subjectA.id!!,
                    status = OpinionStatusEnum.PUBLISHED,
                    timestamp = Instant.now(),
                    mark = null,
                ),
            )
        val listA =
            opinionListRepository.save(
                OpinionListPersistence(owner = USER_ID, name = "A List", privacy = OpinionListPrivacyEnum.SEARCHABLE),
            )
        opinionsAssignmentRepository.save(
            OpinionsToOpinionListsPersistence(opinionList = listA.id!!, opinion = opinionA.id!!, weight = 1.0),
        )

        val result =
            mockMvc
                .perform(
                    get("/api/opinion-lists/search")
                        .header("Authorization", userToken)
                        .param("findThisTextInAnyOfTheAbove", "FindMe")
                        .param("page", "0")
                        .param("size", "10"),
                ).andExpect(status().isOk)
                .andReturn()

        val page = objectMapper.readValue<PageResponseDto<OpinionListSummaryDto>>(result.response.contentAsString)
        assertThat(page.items.map { it.listName }).contains("FindMe List", "S List", "A List")
    }

    @Test
    fun `search by lat-long and radius works`() {
        // We use coordinates that are far from Berlin (where seed data is)
        // Tokyo: 35.6895, 139.6917
        // Yokohama (approx 30km from Tokyo): 35.4437, 139.6380

        val tokyoReferent =
            referentRepository.save(
                ReferentPersistence(
                    name = "Tokyo Spot",
                    address = "Tokyo",
                    latitude = 35.6895,
                    longitude = 139.6917,
                    referentGroup = UUID.randomUUID(),
                ),
            )
        val yokohamaReferent =
            referentRepository.save(
                ReferentPersistence(
                    name = "Yokohama Spot",
                    address = "Yokohama",
                    latitude = 35.4437,
                    longitude = 139.6380,
                    referentGroup = UUID.randomUUID(),
                ),
            )

        val tokyoSubject =
            subjectRepository.save(
                OpinionSubjectPersistence(name = "Tokyo Sub", referent = tokyoReferent.id!!),
            )
        val yokohamaSubject =
            subjectRepository.save(
                OpinionSubjectPersistence(name = "Yokohama Sub", referent = yokohamaReferent.id!!),
            )

        val tokyoOpinion =
            opinionRepository.save(
                OpinionPersistence(
                    owner = USER_ID,
                    subject = tokyoSubject.id!!,
                    status = OpinionStatusEnum.PUBLISHED,
                    timestamp = Instant.now(),
                    mark = null,
                ),
            )
        val yokohamaOpinion =
            opinionRepository.save(
                OpinionPersistence(
                    owner = USER_ID,
                    subject = yokohamaSubject.id!!,
                    status = OpinionStatusEnum.PUBLISHED,
                    timestamp = Instant.now(),
                    mark = null,
                ),
            )

        val tokyoList =
            opinionListRepository.save(
                OpinionListPersistence(
                    owner = USER_ID,
                    name = "Tokyo List",
                    privacy = OpinionListPrivacyEnum.SEARCHABLE,
                ),
            )
        val yokohamaList =
            opinionListRepository.save(
                OpinionListPersistence(
                    owner = USER_ID,
                    name = "Yokohama List",
                    privacy = OpinionListPrivacyEnum.SEARCHABLE,
                ),
            )

        opinionsAssignmentRepository.save(
            OpinionsToOpinionListsPersistence(opinionList = tokyoList.id!!, opinion = tokyoOpinion.id!!, weight = 1.0),
        )
        opinionsAssignmentRepository.save(
            OpinionsToOpinionListsPersistence(
                opinionList = yokohamaList.id!!,
                opinion = yokohamaOpinion.id!!,
                weight = 1.0,
            ),
        )

        // Search within 5km of Tokyo center - should only find Tokyo List
        val result5km =
            mockMvc
                .perform(
                    get("/api/opinion-lists/search")
                        .header("Authorization", userToken)
                        .param("locationFilter.latitude", "35.6895")
                        .param("locationFilter.longitude", "139.6917")
                        .param("locationFilter.radiusInMeters", "5000.0")
                        .param("page", "0")
                        .param("size", "10"),
                ).andExpect(status().isOk)
                .andReturn()

        val page5km = objectMapper.readValue<PageResponseDto<OpinionListSummaryDto>>(result5km.response.contentAsString)
        assertThat(page5km.items.map { it.listName }).containsExactly("Tokyo List")

        // Search within 40km of Tokyo center - should find both
        val result40km =
            mockMvc
                .perform(
                    get("/api/opinion-lists/search")
                        .header("Authorization", userToken)
                        .param("locationFilter.latitude", "35.6895")
                        .param("locationFilter.longitude", "139.6917")
                        .param("locationFilter.radiusInMeters", "40000.0")
                        .param("page", "0")
                        .param("size", "10"),
                ).andExpect(status().isOk)
                .andReturn()

        val page40km =
            objectMapper.readValue<PageResponseDto<OpinionListSummaryDto>>(
                result40km.response.contentAsString,
            )
        assertThat(page40km.items.map { it.listName }).contains("Tokyo List", "Yokohama List")
        assertThat(page40km.items.map { it.listName }).doesNotContain("Berlin List")
    }
}
