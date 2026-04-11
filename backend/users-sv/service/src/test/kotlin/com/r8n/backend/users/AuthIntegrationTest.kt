package com.r8n.backend.users

import com.r8n.backend.users.api.dto.LoginRequestDto
import com.r8n.backend.users.provider.database.PIIRepository
import com.r8n.backend.users.provider.database.UserRepository
import com.r8n.backend.users.provider.database.UserRoleAssignmentRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestObjectMapperConfiguration::class)
class AuthIntegrationTest {
    private companion object {
        @Container
        @ServiceConnection
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer(DockerImageName.parse("postgres:15"))
                .withDatabaseName("users")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("db/init-schema.sql")
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var piiRepository: PIIRepository

    @Autowired
    lateinit var userRoleAssignmentRepository: UserRoleAssignmentRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    @Transactional
    fun `login with valid credentials returns tokens`() {
        val pii = piiRepository.findAll().first { it.email == "test@test.test" }
        val userId = pii.userId
        val encodedPassword = passwordEncoder.encode("1234")

        entityManager
            .createNativeQuery("UPDATE users.users SET password_hash = :passwordHash WHERE id = :userId")
            .setParameter("passwordHash", encodedPassword)
            .setParameter("userId", userId)
            .executeUpdate()

        entityManager
            .createNativeQuery(
                "INSERT INTO users.users_role_assignments (id, \"user\", role, granted_by, timestamp) VALUES (:id, :userId, :role, :grantedBy, :timestamp)",
            ).setParameter("id", UUID.randomUUID())
            .setParameter("userId", userId)
            .setParameter("role", "ADMIN")
            .setParameter("grantedBy", userId)
            .setParameter("timestamp", Instant.now())
            .executeUpdate()

        entityManager.clear()

        val loginRequest = LoginRequestDto("test@test.test", "1234")

        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.expiresInMilliseconds").value(3600000))
    }

    @Test
    fun `login with invalid password returns 401`() {
        val loginRequest = LoginRequestDto("test@test.test", "wrong")

        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `login with unknown user returns 401`() {
        val loginRequest = LoginRequestDto("unknown@test.test", "1234")

        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)),
            ).andExpect(status().isUnauthorized)
    }
}