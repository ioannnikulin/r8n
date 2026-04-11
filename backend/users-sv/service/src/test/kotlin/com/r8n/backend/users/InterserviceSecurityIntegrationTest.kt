package com.r8n.backend.users

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.r8n.backend.security.ServiceTokenService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Instant
import java.util.Date
import java.util.UUID

@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InterserviceSecurityIntegrationTest {
    companion object {
        @Container
        @ServiceConnection
        val postgres =
            PostgreSQLContainer("postgres:15")
                .withDatabaseName("users")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("db/init-schema.sql")
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Value("\${r8n.security.jwt.private-key:}")
    lateinit var privateKeyPem: String

    @Value("\${r8n.security.jwt.issuer:r8n}")
    lateinit var issuer: String

    @Test
    fun `getUserName requires authentication`() {
        val userId = UUID.randomUUID()
        mockMvc
            .perform(get("/users/$userId/name"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `getUserName allows access with SERVICE role`() {
        val userId = UUID.randomUUID()
        val token = generateServiceToken()

        mockMvc
            .perform(
                get("/users/$userId/name")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isOk)
    }

    @Test
    fun `getUserName denies access with USER role`() {
        val userId = UUID.randomUUID()
        val token = generateUserToken()

        mockMvc
            .perform(
                get("/users/$userId/name")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isForbidden)
    }

    private fun generateServiceToken(): String {
        val privateKey = ServiceTokenService.decodePrivateKey(privateKeyPem)
        val signer = RSASSASigner(privateKey)
        val now = Instant.now()
        val claimsSet =
            JWTClaimsSet
                .Builder()
                .issuer(issuer)
                .subject("service-test")
                .claim("roles", listOf("SERVICE"))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .build()
        val signedJWT = SignedJWT(JWSHeader(JWSAlgorithm.RS256), claimsSet)
        signedJWT.sign(signer)
        return signedJWT.serialize()
    }

    private fun generateUserToken(): String {
        val privateKey = ServiceTokenService.decodePrivateKey(privateKeyPem)
        val signer = RSASSASigner(privateKey)
        val now = Instant.now()
        val claimsSet =
            JWTClaimsSet
                .Builder()
                .issuer(issuer)
                .subject("user-test")
                .claim("roles", listOf("USER"))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .build()
        val signedJWT = SignedJWT(JWSHeader(JWSAlgorithm.RS256), claimsSet)
        signedJWT.sign(signer)
        return signedJWT.serialize()
    }
}