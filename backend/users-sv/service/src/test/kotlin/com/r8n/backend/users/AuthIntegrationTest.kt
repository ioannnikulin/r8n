package com.r8n.backend.users

import com.r8n.backend.users.api.dto.LoginRequestDto
import com.r8n.backend.users.persistence.UserPersistence
import com.r8n.backend.users.provider.database.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@ActiveProfiles("test")
@Testcontainers
@AutoConfigureJsonTesters
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestObjectMapperConfiguration::class)
class AuthIntegrationTest {

    private companion object {
        @Container
        @ServiceConnection
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:15"))
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
    lateinit var passwordEncoder: org.springframework.security.crypto.password.PasswordEncoder

    @Test
    fun `login with valid credentials returns tokens`() {
        val user = userRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000000")).get()
        val encodedPassword = passwordEncoder.encode("1234")
        val updatedUser = UserPersistence(
            id = user.id,
            status = user.status,
            statusTimestamp = user.statusTimestamp,
            passwordHash = encodedPassword
        )
        userRepository.save(updatedUser)
        
        println("[DEBUG_LOG] User password hash: ${updatedUser.passwordHash}")
        println("[DEBUG_LOG] Match 1234: ${passwordEncoder.matches("1234", updatedUser.passwordHash)}")
        
        val loginRequest = LoginRequestDto("test@test.test", "1234")
        
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.expiresInMilliseconds").value(3600000))
    }

    @Test
    fun `login with invalid password returns 401`() {
        val loginRequest = LoginRequestDto("test@test.test", "wrong")
        
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `login with unknown user returns 401`() {
        val loginRequest = LoginRequestDto("unknown@test.test", "1234")
        
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isUnauthorized)
    }
}
