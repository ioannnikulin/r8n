package com.r8n.backend.opinions.lists

import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.TestObjectMapperConfiguration
import com.r8n.backend.opinions.api.lists.dto.OpinionListNameAndOwnerDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListNameDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListSummaryDto
import com.r8n.backend.security.ServiceTokenService
import com.r8n.backend.users.integration.api.UsersInternalApi
import com.r8n.backend.users.integration.api.dto.UserDto
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
class OpinionListSearchIntegrationTest {
    private companion object {
        val ANNA_ID: UUID = UUID.fromString("20202020-2020-2020-2020-202020202020")
        val BOB_ID: UUID = UUID.fromString("30303030-3030-3030-3030-303030303030")
        
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
        whenever(usersInternalApi.getUserName(any())).thenReturn("Anna Müller")
    }

    @Test
    fun `search by name substring works`() {
        val result =
            mockMvc
                .perform(
                    get("/api/opinion-lists/search")
                        .header("Authorization", annaToken)
                        .param("nameSubstring", "l11")
                        .param("page", "0")
                        .param("size", "10")
                ).andExpect(status().isOk)
                .andReturn()

        val page = objectMapper.readValue<PageResponseDto<OpinionListSummaryDto>>(result.response.contentAsString)
        assertThat(page.items.map { it.listName }).contains("l11")
    }

    @Test
    fun `sorting works in search`() {
        // Assume seed data has multiple lists. We'll sort by name DESC.
        val result =
            mockMvc
                .perform(
                    get("/api/opinion-lists/search")
                        .header("Authorization", annaToken)
                        .param("nameSubstring", "l")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort[0].property", "listName")
                        .param("sort[0].direction", "DESC")
                ).andExpect(status().isOk)
                .andReturn()

        val page = objectMapper.readValue<PageResponseDto<OpinionListSummaryDto>>(result.response.contentAsString)
        val names = page.items.map { it.listName.lowercase() }
        assertThat(names).isEqualTo(names.sortedDescending())
    }

    @Test
    fun `pagination with filtering and multi-factor sorting works`() {
        // 1. Pre-seed 10 lists
        val prefix = "Alpha "
        for (i in 1..10) {
            val finalName = if (i <= 8) "$prefix$i" else "Beta $i"
            
            mockMvc.perform(
                post("/api/opinion-lists")
                    .header("Authorization", annaToken)
                    .param("name", finalName)
                    .param("privacy", "SEARCHABLE")
            ).andExpect(status().isOk)
        }

        // Now we have 8 lists starting with "Alpha".
        // Let's check: request page 2, size 3.
        // Total matching: 8.
        // Page 0 (size 3): 1, 2, 3
        // Page 1 (size 3): 4, 5, 6
        // Page 2 (size 3): 7, 8
        
        // Let's add sorting: by ownerName ASC, then by listName DESC.
        // Since all new lists have the same owner, it will effectively sort by listName DESC.
        // "Alpha 8", "Alpha 7", "Alpha 6", "Alpha 5", "Alpha 4", "Alpha 3", "Alpha 2", "Alpha 1"
        
        // Page 0: "Alpha 8", "Alpha 7", "Alpha 6"
        // Page 1: "Alpha 5", "Alpha 4", "Alpha 3"
        // Page 2: "Alpha 2", "Alpha 1"
        
        val result = mockMvc.perform(
            get("/api/opinion-lists/search")
                .header("Authorization", annaToken)
                .param("nameSubstring", "Alpha")
                .param("page", "2")
                .param("size", "3")
                .param("sort[0].property", "ownerName")
                .param("sort[0].direction", "ASC")
                .param("sort[1].property", "listName")
                .param("sort[1].direction", "DESC")
        ).andExpect(status().isOk)
        .andReturn()

        val page = objectMapper.readValue<PageResponseDto<OpinionListSummaryDto>>(result.response.contentAsString)
        
        assertThat(page.total).isEqualTo(8)
        assertThat(page.items).hasSize(2)
        assertThat(page.items.map { it.listName }).containsExactly("Alpha 2", "Alpha 1")
    }

    @Test
    fun `search by authorId works`() {
        // Anna owns l11, l12, l13 (from seed data)
        val result =
            mockMvc
                .perform(
                    get("/api/opinion-lists/search")
                        .header("Authorization", annaToken)
                        .param("authorId", ANNA_ID.toString())
                        .param("page", "0")
                        .param("size", "10")
                ).andExpect(status().isOk)
                .andReturn()

        val page = objectMapper.readValue<PageResponseDto<OpinionListSummaryDto>>(result.response.contentAsString)
        assertThat(page.items.map { it.listName }).contains("l11", "l12", "l13")
        assertThat(page.items).allSatisfy { assertThat(it.owner).isEqualTo(ANNA_ID) }
    }

    @Test
    fun `search by authorNameSubstring works via usersApi`() {
        val BERNARD_ID = UUID.fromString("10101010-1010-1010-1010-101010101010")
        val mockUserDto = org.mockito.kotlin.mock<UserDto>()
        whenever(mockUserDto.id).thenReturn(BERNARD_ID)
        whenever(usersInternalApi.findUsersByNameSubstring("Bernard")).thenReturn(listOf(mockUserDto))

        // Bernard owns l21, l22, l23 (from seed data)
        val result =
            mockMvc
                .perform(
                    get("/api/opinion-lists/search")
                        .header("Authorization", annaToken)
                        .param("authorNameSubstring", "Bernard")
                        .param("page", "0")
                        .param("size", "10")
                ).andExpect(status().isOk)
                .andReturn()

        val page = objectMapper.readValue<PageResponseDto<OpinionListSummaryDto>>(result.response.contentAsString)
        assertThat(page.items.map { it.listName }).contains("l21", "l22", "l23")
        assertThat(page.items).allSatisfy { assertThat(it.owner).isEqualTo(BERNARD_ID) }
    }

    @Test
    fun `search with multiple filters (AND logic) works`() {
        val result =
            mockMvc
                .perform(
                    get("/api/opinion-lists/search")
                        .header("Authorization", annaToken)
                        .param("nameSubstring", "l11")
                        .param("authorId", ANNA_ID.toString())
                        .param("page", "0")
                        .param("size", "10")
                ).andExpect(status().isOk)
                .andReturn()

        val page = objectMapper.readValue<PageResponseDto<OpinionListSummaryDto>>(result.response.contentAsString)
        assertThat(page.items.map { it.listName }).containsExactly("l11")
    }

    @Test
    fun `search shows own private lists but hides others private lists`() {
        // Seed data: 
        // l11, l12, l13 are SEARCHABLE, owned by Anna (20202020-...)
        // l21, l22, l23 are SEARCHABLE, owned by Bernard (10101010-...)
        // l24 is PRIVATE, owned by Bernard (10101010-...)
        
        // a private list for Anna to test she can see it
        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/opinion-lists")
                    .header("Authorization", annaToken)
                    .param("name", "Anna's secret list")
                    .param("privacy", "PRIVATE")
            ).andExpect(status().isOk)

        // Anna searches for lists:
        val result =
            mockMvc
                .perform(
                    get("/api/opinion-lists/search")
                        .header("Authorization", annaToken)
                        .param("nameSubstring", "l")
                        .param("page", "0")
                        .param("size", "50")
                ).andExpect(status().isOk)
                .andReturn()

        val page = objectMapper.readValue<PageResponseDto<OpinionListSummaryDto>>(result.response.contentAsString)
        val names = page.items.map { it.listName }
        
        assertThat(names).contains("l11", "l12", "l13") // Anna's searchable lists
        assertThat(names).contains("l21", "l22", "l23") // Bernard's searchable lists
        assertThat(names).doesNotContain("l24") // Bernard's private list
    }

    @Test
    fun `getApprovedListsWithNamesAndOwners works`() {
        val result =
            mockMvc
                .perform(
                    get("/api/opinion-lists/approved")
                        .header("Authorization", annaToken)
                        .param("page", "0")
                        .param("size", "50")
                ).andExpect(status().isOk)
                .andReturn()

        val page = objectMapper.readValue<PageResponseDto<OpinionListNameAndOwnerDto>>(result.response.contentAsString)
        val names = page.items.map { it.listName }
        // Now it only contains lists with ACCEPTED access requests, excluding own lists (unless there's an explicit request)
        assertThat(names).contains("l21", "l22", "l23", "l31")
        assertThat(names).doesNotContain("l11", "l12", "l13") // Own lists not in approved access requests
        assertThat(names).doesNotContain("l24") // private list without access
    }

    @Test
    fun `getMineNamesOnly works`() {
        val result =
            mockMvc
                .perform(
                    get("/api/opinion-lists/mine/names")
                        .header("Authorization", annaToken)
                        .param("page", "0")
                        .param("size", "50")
                ).andExpect(status().isOk)
                .andReturn()

        val page = objectMapper.readValue<PageResponseDto<OpinionListNameDto>>(result.response.contentAsString)
        val names = page.items.map { it.name }
        assertThat(names).contains("l11", "l12", "l13")
        assertThat(names).doesNotContain("l21") // Bernard's list
    }
}
