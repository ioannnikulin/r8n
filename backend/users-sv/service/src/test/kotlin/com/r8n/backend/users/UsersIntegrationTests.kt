package com.r8n.backend.users

import com.r8n.backend.users.api.dto.UserProfileDto
import com.r8n.backend.users.api.dto.UserStatusEnumDto
import com.r8n.backend.users.service.TokenService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID

@ActiveProfiles("test")
@Testcontainers
@AutoConfigureJsonTesters
@AutoConfigureMockMvc
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
)
@Import(TestObjectMapperConfiguration::class)
class UsersIntegrationTests {
    private companion object {
        @Suppress("unused") // used to store test database container
        @Container
        @ServiceConnection
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer(DockerImageName.parse("postgres:15"))
                .withDatabaseName("users")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("db/init-schema.sql")

        const val USER_ID = "00000000-0000-0000-0000-000000000000"
        val PNG_BYTES: ByteArray =
            Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMB/axq6L8AAAAASUVORK5CYII=",
            )
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var tokenService: TokenService

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update("DELETE FROM users.profile_avatars")
        // Profile update tests mutate the seeded user, so reset public profile fields
        // to keep this test class independent from method execution order.
        jdbcTemplate.update(
            "UPDATE users.pii SET name = ?, about = ?, location = ? WHERE user_id = ?",
            "Test Testsson",
            "I am a coffee expert",
            "Berlin, Germany",
            UUID.fromString(USER_ID),
        )
    }

    private fun userAccessToken() = tokenService.generateAccessToken(UUID.fromString(USER_ID), listOf("USER"))

    @Test
    @WithMockUser(username = USER_ID)
    fun `getUserProfile returns profile with last seen timestamp`() {
        // Given
        val userId = UUID.fromString("10101010-1010-1010-1010-101010101010")
        val lastSeenAt = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS)

        jdbcTemplate.update(
            "UPDATE users.users SET last_seen_at = ? WHERE id = ?",
            Timestamp.from(lastSeenAt),
            userId,
        )

        val accessToken = tokenService.generateAccessToken(UUID.fromString(USER_ID), listOf("USER"))

        // When
        val result =
            mockMvc
                .perform(
                    get("/api/users/$userId")
                        .header("Authorization", "Bearer $accessToken"),
                ).andExpect(status().isOk)
                .andReturn()

        val actual: UserProfileDto = objectMapper.readValue(result.response.contentAsString)

        // Then
        assertEquals(userId, actual.id)
        assertEquals("coffee expert Bernard", actual.name)
        assertEquals(UserStatusEnumDto.ACTIVE, actual.status)
        assertEquals("I am a bratwurst expert", actual.about)
        assertEquals("Munich, Germany", actual.location)

        assertTrue(actual.lastSeenAt != null, "lastSeenAt should not be null")
        assertEquals(lastSeenAt, actual.lastSeenAt)
    }

    @Test
    fun `update my profile persists normalized public profile fields`() {
        val requestBody =
            objectMapper.writeValueAsString(
                mapOf(
                    "name" to "  Updated Testsson  ",
                    "about" to "  Privacy-conscious coffee reviewer  ",
                    "location" to "  Hamburg, Germany  ",
                ),
            )

        val result =
            mockMvc
                .perform(
                    patch("/api/users/me/public-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${userAccessToken()}"),
                ).andExpect(status().isOk)
                .andReturn()

        val actual: UserProfileDto = objectMapper.readValue(result.response.contentAsString)
        assertEquals(UUID.fromString(USER_ID), actual.id)
        assertEquals("Updated Testsson", actual.name)
        assertEquals("Privacy-conscious coffee reviewer", actual.about)
        assertEquals("Hamburg, Germany", actual.location)

        val persisted =
            jdbcTemplate.queryForMap(
                "SELECT name, about, location FROM users.pii WHERE user_id = ?",
                UUID.fromString(USER_ID),
            )
        assertEquals("Updated Testsson", persisted["name"])
        assertEquals("Privacy-conscious coffee reviewer", persisted["about"])
        assertEquals("Hamburg, Germany", persisted["location"])
    }

    @Test
    fun `update my profile stores blank optional fields as null`() {
        val requestBody =
            objectMapper.writeValueAsString(
                mapOf(
                    "name" to "Test Testsson",
                    "about" to "   ",
                    "location" to "",
                ),
            )

        val result =
            mockMvc
                .perform(
                    patch("/api/users/me/public-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${userAccessToken()}"),
                ).andExpect(status().isOk)
                .andReturn()

        val actual: UserProfileDto = objectMapper.readValue(result.response.contentAsString)
        assertEquals(null, actual.about)
        assertEquals(null, actual.location)
    }

    @Test
    fun `update my profile rejects blank display name`() {
        val requestBody =
            objectMapper.writeValueAsString(
                mapOf(
                    "name" to "   ",
                    "about" to "About",
                    "location" to "Berlin",
                ),
            )

        mockMvc
            .perform(
                patch("/api/users/me/public-profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${userAccessToken()}"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `update my profile rejects values longer than public profile limits`() {
        val requestBody =
            objectMapper.writeValueAsString(
                mapOf(
                    "name" to "Test Testsson",
                    "about" to "a".repeat(256),
                    "location" to "Berlin",
                ),
            )

        mockMvc
            .perform(
                patch("/api/users/me/public-profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${userAccessToken()}"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `update my profile rejects duplicate display name`() {
        val requestBody =
            objectMapper.writeValueAsString(
                mapOf(
                    "name" to "coffee expert Bernard",
                    "about" to "About",
                    "location" to "Berlin",
                ),
            )

        mockMvc
            .perform(
                patch("/api/users/me/public-profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${userAccessToken()}"),
            ).andExpect(status().isConflict)
    }

    @Test
    fun `avatar upload and fetch returns image bytes`() {
        val avatar =
            MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                PNG_BYTES,
            )

        mockMvc
            .perform(
                multipart("/api/users/me/avatar")
                    .file(avatar)
                    .with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${userAccessToken()}"),
            ).andExpect(status().isNoContent)

        mockMvc
            .perform(
                get("/api/users/$USER_ID/avatar")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${userAccessToken()}"),
            ).andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE))
            .andExpect(content().bytes(PNG_BYTES))
    }

    @Test
    fun `avatar upload rejects unsupported content type`() {
        val avatar =
            MockMultipartFile(
                "file",
                "avatar.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "not an image".toByteArray(),
            )

        mockMvc
            .perform(
                multipart("/api/users/me/avatar")
                    .file(avatar)
                    .with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${userAccessToken()}"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `avatar delete removes current user avatar`() {
        val avatar =
            MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                PNG_BYTES,
            )

        mockMvc
            .perform(
                multipart("/api/users/me/avatar")
                    .file(avatar)
                    .with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${userAccessToken()}"),
            ).andExpect(status().isNoContent)

        mockMvc
            .perform(
                delete("/api/users/me/avatar")
                    .with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${userAccessToken()}"),
            ).andExpect(status().isNoContent)

        mockMvc
            .perform(
                get("/api/users/$USER_ID/avatar")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${userAccessToken()}"),
            ).andExpect(status().isNoContent)
    }
}
