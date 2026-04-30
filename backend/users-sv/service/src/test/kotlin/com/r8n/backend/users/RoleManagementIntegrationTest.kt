package com.r8n.backend.users

import com.r8n.backend.opinions.api.access.IncomingAccessRequestApi
import com.r8n.backend.opinions.api.access.OutgoingAccessRequestApi
import com.r8n.backend.opinions.integration.api.OpinionListsInternalApi
import com.r8n.backend.users.api.dto.RoleEnumDto
import com.r8n.backend.users.service.TokenService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestObjectMapperConfiguration::class)
@MockitoBean(
    types = [
        IncomingAccessRequestApi::class,
        OutgoingAccessRequestApi::class,
        OpinionListsInternalApi::class,
    ],
)
class RoleManagementIntegrationTest {
    companion object {
        @Suppress("unused")
        @Container
        @ServiceConnection
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer("postgres:15")
                .withDatabaseName("users")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("db/init-schema.sql")

        const val ADMIN_ID = "00000000-0000-0000-0000-000000000000"
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var tokenService: TokenService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update("DELETE FROM users.users_role_assignments WHERE \"user\" <> '${ADMIN_ID}'")
    }

    private fun adminToken() = tokenService.generateAccessToken(UUID.fromString(ADMIN_ID), listOf("ADMIN"))

    private fun assignRole(
        adminToken: String,
        userId: UUID,
        role: RoleEnumDto,
    ) {
        mockMvc
            .perform(
                post("/api/admin/users/$userId/roles")
                    .with(csrf())
                    .header("Authorization", "Bearer $adminToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("role" to role.name))),
            ).andExpect(status().isNoContent)
    }

    private fun seedUser(userId: UUID) {
        jdbcTemplate.update(
            "INSERT INTO users.users (id, status, status_timestamp) VALUES (?, 'ACTIVE', NOW()) ON CONFLICT DO NOTHING",
            userId,
        )
        jdbcTemplate.update(
            "INSERT INTO users.pii (user_id, name, email) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
            userId,
            "User $userId",
            "$userId@test.test",
        )
    }

    // --- isHumanModerator hierarchy ---

    @Test
    fun `isHumanModerator returns true for a user with only SUPPORT role`() {
        val supportUserId = UUID.randomUUID()
        seedUser(supportUserId)
        assignRole(adminToken(), supportUserId, RoleEnumDto.SUPPORT)

        mockMvc
            .perform(
                get("/api/internal/users/$supportUserId/is-human-moderator")
                    .header("Authorization", "Bearer ${adminToken()}"),
            ).andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @Test
    fun `isHumanModerator returns true for a user with only ADMIN role`() {
        val adminUserId = UUID.randomUUID()
        seedUser(adminUserId)
        assignRole(adminToken(), adminUserId, RoleEnumDto.ADMIN)

        mockMvc
            .perform(
                get("/api/internal/users/$adminUserId/is-human-moderator")
                    .header("Authorization", "Bearer ${adminToken()}"),
            ).andExpect(status().isOk)
            .andExpect(content().string("true"))
    }

    @Test
    fun `isHumanModerator returns false for a plain USER`() {
        val userId = UUID.randomUUID()
        seedUser(userId)

        mockMvc
            .perform(
                get("/api/internal/users/$userId/is-human-moderator")
                    .header("Authorization", "Bearer ${adminToken()}"),
            ).andExpect(status().isOk)
            .andExpect(content().string("false"))
    }

    // --- last-admin guard ---

    @Test
    fun `revokeRole blocks removal when only active admin remains even if a deleted user has ADMIN`() {
        val deletedUserId = UUID.randomUUID()
        seedUser(deletedUserId)
        assignRole(adminToken(), deletedUserId, RoleEnumDto.ADMIN)
        jdbcTemplate.update(
            "UPDATE users.users SET status = 'DELETED' WHERE id = ?",
            deletedUserId,
        )

        val secondAdminId = UUID.randomUUID()
        mockMvc
            .perform(
                delete("/api/admin/users/$ADMIN_ID/roles/ADMIN")
                    .with(csrf())
                    .header(
                        "Authorization",
                        "Bearer ${tokenService.generateAccessToken(secondAdminId, listOf("ADMIN"))}",
                    ),
            ).andExpect(status().isUnprocessableEntity)
    }
}
