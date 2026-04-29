package com.r8n.backend.opinions.lists

import com.r8n.backend.opinions.TestObjectMapperConfiguration
import com.r8n.backend.opinions.access.database.AccessRequestRepository
import com.r8n.backend.opinions.access.domain.RequestStatusEnum
import com.r8n.backend.opinions.api.lists.dto.OpinionListDto
import com.r8n.backend.opinions.lists.database.OpinionListRepository
import com.r8n.backend.opinions.lists.database.OpinionListSyncRepository
import com.r8n.backend.security.ServiceTokenService
import com.r8n.backend.users.integration.api.UsersInternalApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@Import(TestObjectMapperConfiguration::class)
class OpinionListSyncIntegrationTest {
    private companion object {
        val BERNARD_ID: UUID = UUID.fromString("10101010-1010-1010-1010-101010101010")
        val ANNA_ID: UUID = UUID.fromString("20202020-2020-2020-2020-202020202020")

        val BERNARD_LIST_ID: UUID = UUID.fromString("70000000-0000-0000-0000-000000000001")
        val ANNA_LIST_ID: UUID = UUID.fromString("70000000-0000-0000-0000-000000000003")

        val PRESEEDED_REQUEST_ID: UUID = UUID.fromString("30000000-0000-0000-0000-300000000006")

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
    lateinit var objectMapper: JsonMapper

    @Autowired
    lateinit var serviceTokenService: ServiceTokenService

    @Autowired
    lateinit var opinionListRepository: OpinionListRepository

    @Autowired
    lateinit var opinionListSyncRepository: OpinionListSyncRepository

    @Autowired
    lateinit var accessRequestRepository: AccessRequestRepository

    @MockitoBean
    lateinit var usersInternalApi: UsersInternalApi

    lateinit var requesterToken: String

    @BeforeEach
    fun setUp() {
        requesterToken = "Bearer " + serviceTokenService.generateAccessToken(ANNA_ID, listOf("USER"))

        whenever(usersInternalApi.isAnyModerator(any())).thenReturn(false)
        whenever(usersInternalApi.isHumanModerator(any())).thenReturn(false)
        whenever(usersInternalApi.getUserName(any())).thenReturn("Anna Müller")

        // Clean up syncs and links created in tests
        val l11Id = UUID.fromString("80000000-0000-0000-0000-000000000111")
        val l31Id = UUID.fromString("80000000-0000-0000-0000-000000000311")
        val r31Id = UUID.fromString("40000000-0000-0000-0000-000000000031")

        // Reset sync weight to 1.0 (preseeded value)
        mockMvc.perform(
            post("/api/opinion-lists/$l11Id/sync")
                .header("Authorization", requesterToken)
                .param("addedListId", l31Id.toString())
                .param("weight", "1.0"),
        )

        // Remove r31 from l11 if it was added
        mockMvc.perform(
            patch("/api/opinion-lists/$l11Id/unlink")
                .header("Authorization", requesterToken)
                .param("opinionId", r31Id.toString()),
        )
    }

    @Test
    fun `sync and unsync with opinion list works`() {
        // 1. Bernard's list is the source (preseeded)
        // 2. Anna's list is the destination (preseeded)

        // 3. Sync without approved access request - should fail (it's currently SENT)
        mockMvc
            .perform(
                post("/api/opinion-lists/$ANNA_LIST_ID/sync")
                    .header("Authorization", requesterToken)
                    .param("addedListId", BERNARD_LIST_ID.toString()),
            ).andExpect(status().isForbidden)

        // 4. Approve the preseeded access request
        val request = accessRequestRepository.findById(PRESEEDED_REQUEST_ID).get()
        request.status = RequestStatusEnum.ACCEPTED
        accessRequestRepository.save(request)

        // 5. Sync with access request - should work
        val syncResult =
            mockMvc
                .perform(
                    post("/api/opinion-lists/$ANNA_LIST_ID/sync")
                        .header("Authorization", requesterToken)
                        .param("addedListId", BERNARD_LIST_ID.toString()),
                ).andExpect(status().isOk)
                .andReturn()
                .response.contentAsString
                .let { objectMapper.readValue<OpinionListDto>(it) }

        assertEquals(ANNA_LIST_ID, syncResult.id)
        val syncPersistence =
            opinionListSyncRepository.findByDestinationListAndSourceList(
                ANNA_LIST_ID,
                BERNARD_LIST_ID,
            )
        assertTrue(syncPersistence != null)
        assertEquals(1.0, syncPersistence?.weight)

        // 5.1 Update weight
        mockMvc
            .perform(
                post("/api/opinion-lists/$ANNA_LIST_ID/sync")
                    .header("Authorization", requesterToken)
                    .param("addedListId", BERNARD_LIST_ID.toString())
                    .param("weight", "0.5"),
            ).andExpect(status().isOk)

        val updatedSyncPersistence =
            opinionListSyncRepository.findByDestinationListAndSourceList(
                ANNA_LIST_ID,
                BERNARD_LIST_ID,
            )
        assertEquals(0.5, updatedSyncPersistence?.weight)

        // 6. Unsync
        val unsyncResult =
            mockMvc
                .perform(
                    post("/api/opinion-lists/$ANNA_LIST_ID/unsync")
                        .header("Authorization", requesterToken)
                        .param("removedListId", BERNARD_LIST_ID.toString()),
                ).andExpect(status().isOk)
                .andReturn()
                .response.contentAsString
                .let { objectMapper.readValue<OpinionListDto>(it) }

        assertEquals(ANNA_LIST_ID, unsyncResult.id)
        assertTrue(
            opinionListSyncRepository.findByDestinationListAndSourceList(ANNA_LIST_ID, BERNARD_LIST_ID) == null,
        )
    }

    @Test
    fun `cannot sync to a list you do not own`() {
        mockMvc
            .perform(
                post("/api/opinion-lists/$BERNARD_LIST_ID/sync")
                    .header("Authorization", requesterToken)
                    .param("addedListId", ANNA_LIST_ID.toString()),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `sync with invalid weight should return bad request`() {
        // Approve the preseeded access request
        val request = accessRequestRepository.findById(PRESEEDED_REQUEST_ID).get()
        request.status = RequestStatusEnum.ACCEPTED
        accessRequestRepository.save(request)

        // Weight < 0
        mockMvc
            .perform(
                post("/api/opinion-lists/$ANNA_LIST_ID/sync")
                    .header("Authorization", requesterToken)
                    .param("addedListId", BERNARD_LIST_ID.toString())
                    .param("weight", "-0.1"),
            ).andExpect(status().isBadRequest)

        // Weight > 1
        mockMvc
            .perform(
                post("/api/opinion-lists/$ANNA_LIST_ID/sync")
                    .header("Authorization", requesterToken)
                    .param("addedListId", BERNARD_LIST_ID.toString())
                    .param("weight", "1.1"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `complex ownership and sync schema works`() {
        // u1 looks at his l11
        // u1: l11(r11, r12)
        // l21(r23, r24), l22(empty), l31(r31) synced to l11
        // Total for l11: r11(s1), r12(s2), r23(s3), r24(s4), r31(s1)

        val u1Token = "Bearer " + serviceTokenService.generateAccessToken(ANNA_ID, listOf("USER"))
        val l11Id = UUID.fromString("80000000-0000-0000-0000-000000000111")

        val result =
            mockMvc
                .perform(
                    get("/api/opinion-lists/$l11Id")
                        .header("Authorization", u1Token),
                ).andExpect(status().isOk)
                .andReturn()

        val json = result.response.contentAsString
        val list = objectMapper.readValue(json, OpinionListDto::class.java)

        // s1 (r11, r31), s2 (r12), s3 (r23), s4 (r24)
        assertThat(list.opinionSummaries).hasSize(4)

        val s1 = list.opinionSummaries.find { it.subjectName == "Subject 1" }!!
        assertThat(s1.ownMark).isEqualTo(1.1)
        assertThat(s1.opinions).hasSize(2)
        assertThat(s1.opinions.map { it.opinionId }).containsExactlyInAnyOrder(
            UUID.fromString("40000000-0000-0000-0000-000000000011"),
            UUID.fromString("40000000-0000-0000-0000-000000000031"),
        )

        val s2 = list.opinionSummaries.find { it.subjectName == "Subject 2" }!!
        assertThat(s2.ownMark).isEqualTo(1.2)
        assertThat(s2.opinions).hasSize(1)

        val s3 = list.opinionSummaries.find { it.subjectName == "Subject 3" }!!
        assertThat(s3.ownMark).isNull()
        assertThat(s3.opinions).hasSize(1)
        assertThat(s3.opinions[0].opinionId).isEqualTo(UUID.fromString("40000000-0000-0000-0000-000000000023"))

        val s4 = list.opinionSummaries.find { it.subjectName == "Subject 4" }!!
        assertThat(s4.ownMark).isNull()
        assertThat(s4.opinions).hasSize(1)
        assertThat(s4.opinions[0].opinionId).isEqualTo(UUID.fromString("40000000-0000-0000-0000-000000000024"))
    }

    @Test
    fun `no transitive connections - only direct reviews are synced`() {
        // u1 looks at l11
        // u1: l11(r11, r12)
        // l31(r31) synced to l11
        // l21(r23, r24) synced to l11

        // Total for l11: r11, r12, r31, r23, r24

        // l21 has l11 and l12 synced to it.
        // IF it were transitive, l11 would see its own reviews AGAIN via l21, and also r11 via l12 synced to l21.

        val u1Token = "Bearer " + serviceTokenService.generateAccessToken(ANNA_ID, listOf("USER"))
        val l11Id = UUID.fromString("80000000-0000-0000-0000-000000000111")

        val result =
            mockMvc
                .perform(
                    get("/api/opinion-lists/$l11Id")
                        .header("Authorization", u1Token),
                ).andExpect(status().isOk)
                .andReturn()

        val json = result.response.contentAsString
        val list = objectMapper.readValue(json, OpinionListDto::class.java)

        // Subject 1 (r11, r31), Subject 2 (r12), Subject 3 (r23), Subject 4 (r24)
        assertThat(list.opinionSummaries).hasSize(4)

        val s1 = list.opinionSummaries.find { it.subjectName == "Subject 1" }!!
        // r11 (direct), r31 (synced from l31)
        // it should NOT have r11 again (synced from l21 <- l11) or r11 (synced from l21 <- l12)
        assertThat(s1.opinions).hasSize(2)
        assertThat(s1.opinions.map { it.opinionId }).containsExactlyInAnyOrder(
            UUID.fromString("40000000-0000-0000-0000-000000000011"),
            UUID.fromString("40000000-0000-0000-0000-000000000031"),
        )
    }

    @Test
    fun `revoking access to source list breaks sync visibility`() {
        // Anna (u1) has preseeded sync from Bernard's list (l21) to her list (l11)
        // AND she has access (ACCEPTED) in the preseed V12.

        val u1Token = "Bearer " + serviceTokenService.generateAccessToken(ANNA_ID, listOf("USER"))
        val l11Id = UUID.fromString("80000000-0000-0000-0000-000000000111")
        val l21Id = UUID.fromString("80000000-0000-0000-0000-000000000211")

        // 1. Initially it works
        mockMvc
            .perform(get("/api/opinion-lists/$l11Id").header("Authorization", u1Token))
            .andExpect(status().isOk)

        // 2. Revoke access (delete the access request)
        accessRequestRepository.findAll().forEach {
            if (it.requester == ANNA_ID && it.list == l21Id) {
                accessRequestRepository.delete(it)
            }
        }

        // 3. Now getting l11 should still work but r23/r24 (from l21) should be missing
        val result =
            mockMvc
                .perform(get("/api/opinion-lists/$l11Id").header("Authorization", u1Token))
                .andExpect(status().isOk)
                .andReturn()

        val list = objectMapper.readValue(result.response.contentAsString, OpinionListDto::class.java)
        // Summaries for Subject 3 and 4 should be gone (they were only from l21)
        assertThat(list.opinionSummaries.map { it.subjectName }).doesNotContain("Subject 3", "Subject 4")
        // Subject 1 should only have r11, not r31 (wait, r31 was from l31. Let's check l31 access)
        // In V12, l31 synced to l11 also has a preseeded access request.
    }

    @Test
    fun `circular sync works without infinite loop`() {
        // Preseed: l21 synced to l11, l11 synced to l21
        val u1Token = "Bearer " + serviceTokenService.generateAccessToken(ANNA_ID, listOf("USER"))
        val l11Id = UUID.fromString("80000000-0000-0000-0000-000000000111")

        // Should just work and return one level of sync
        mockMvc
            .perform(get("/api/opinion-lists/$l11Id").header("Authorization", u1Token))
            .andExpect(status().isOk)
    }

    @Test
    fun `syncing with private list without access should return not found`() {
        // Bernard has a private list l24
        val l23Id = UUID.fromString("80000000-0000-0000-0000-000000000223")

        // Anna tries to sync her list l11 with Bernard's private list l23
        // It returns 404 because for a private list, if you don't have access, we return 404 to avoid leaking existence
        mockMvc
            .perform(
                post("/api/opinion-lists/$ANNA_LIST_ID/sync")
                    .header("Authorization", requesterToken)
                    .param("addedListId", l23Id.toString()),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `weight aggregation math works correctly`() {
        // Subject 1 has two opinions in l11:
        // 1. r11 (own) with mark 1.1, direct weight 1.0 (preseeded)
        // 2. r31 (synced from l31) with mark 3.1, direct weight 1.0 (preseeded)
        // Global sync weight for l31 -> l11 is 1.0 (preseeded)

        val u1Token = "Bearer " + serviceTokenService.generateAccessToken(ANNA_ID, listOf("USER"))
        val l11Id = UUID.fromString("80000000-0000-0000-0000-000000000111")
        val l31Id = UUID.fromString("80000000-0000-0000-0000-000000000311")

        // Set l31 -> l11 sync weight to 0.5
        mockMvc
            .perform(
                post("/api/opinion-lists/$l11Id/sync")
                    .header("Authorization", u1Token)
                    .param("addedListId", l31Id.toString())
                    .param("weight", "0.5"),
            ).andExpect(status().isOk)

        val result =
            mockMvc
                .perform(get("/api/opinion-lists/$l11Id").header("Authorization", u1Token))
                .andExpect(status().isOk)
                .andReturn()

        val list = objectMapper.readValue(result.response.contentAsString, OpinionListDto::class.java)
        val s1 = list.opinionSummaries.find { it.subjectName == "Subject 1" }!!

        // Expected weights:
        // r11: 1.0 (own item weight is always 1.0)
        // r31: 1.0 (requester weight, default) * 0.5 (sync weight) = 0.5

        // componentMark should be weighted average: (1.1 * 1.0 + 3.1 * 0.5) / (1.0 + 0.5)
        // (1.1 + 1.55) / 1.5 = 2.65 / 1.5 = 1.7666...

        assertThat(
            s1.opinions
                .find {
                    it.opinionId == UUID.fromString("40000000-0000-0000-0000-000000000011")
                }?.weight,
        ).isEqualTo(1.0)
        assertThat(
            s1.opinions
                .find {
                    it.opinionId == UUID.fromString("40000000-0000-0000-0000-000000000031")
                }?.weight,
        ).isEqualTo(0.5)

        // Currently it's a weighted average.
        assertThat(
            s1.componentMark,
        ).isCloseTo(
            1.7666666666666666,
            org.assertj.core.api.Assertions
                .within(0.000000000000001),
        )
    }

    @Test
    fun `requester weight override works`() {
        // r31 is in l31 with weight 1.0 (preseeded)
        // Anna owns l11 and syncs l31 into it with weight 0.5.
        // If she didn't link it manually, weight would be 1.0 (source weight) * 0.5 (sync) = 0.5

        // Now Anna adds r31 to HER list l11 with weight 0.2
        val u1Token = "Bearer " + serviceTokenService.generateAccessToken(ANNA_ID, listOf("USER"))
        val l11Id = UUID.fromString("80000000-0000-0000-0000-000000000111")
        val l31Id = UUID.fromString("80000000-0000-0000-0000-000000000311")
        val r31Id = UUID.fromString("40000000-0000-0000-0000-000000000031")

        // First, set sync weight to 0.5
        mockMvc
            .perform(
                post("/api/opinion-lists/$l11Id/sync")
                    .header("Authorization", u1Token)
                    .param("addedListId", l31Id.toString())
                    .param("weight", "0.5"), // Anna half-trusts u3 generally
            ).andExpect(status().isOk)

        // Add r31 to l11 with weight 0.2
        mockMvc
            .perform(
                post("/api/opinion-lists/$l11Id/link")
                    .header("Authorization", u1Token)
                    .param("opinionId", r31Id.toString())
                    .param("weight", "0.2"), // Anna explicitly sets weight to 0.2
            ).andExpect(status().isOk)

        val result =
            mockMvc
                .perform(get("/api/opinion-lists/$l11Id").header("Authorization", u1Token))
                .andExpect(status().isOk)
                .andReturn()

        val list = objectMapper.readValue(result.response.contentAsString, OpinionListDto::class.java)
        val s1 = list.opinionSummaries.find { it.subjectName == "Subject 1" }!!

        // Expected weights for r31:
        // Option A: Strict Override - the manual link (0.2) completely overrides the synced link.
        // So r31 should only appear once with weight 0.2.

        val r31References = s1.opinions.filter { it.opinionId == r31Id }
        assertThat(r31References).hasSize(1)
        assertThat(r31References[0].weight).isEqualTo(0.2)

        // r11 is also there: mark 1.1, weight 1.0 (own item weight is always 1.0)
        // componentMark = (1.1 * 1.0 + 3.1 * 0.2) / (1.0 + 0.2)
        // (1.1 + 0.62) / 1.2 = 1.72 / 1.2 = 1.4333...

        assertThat(
            s1.componentMark,
        ).isCloseTo(
            1.4333333333333333,
            org.assertj.core.api.Assertions
                .within(0.000000000000001),
        )
    }
}
