package com.r8n.backend.opinions

import com.r8n.backend.mock.integration.UserClient
import com.r8n.backend.opinions.domain.OpinionStatusEnum
import com.r8n.backend.opinions.persistence.OpinionPersistence
import com.r8n.backend.opinions.provider.database.OpinionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.UUID

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
class OpinionRepositoryTest {

    private companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:15"))
            .withDatabaseName("opinions")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/init-schema.sql")
    }

    @Autowired
    lateinit var opinionRepository: OpinionRepository

    @MockitoBean
    lateinit var userClient: UserClient

    @Test
    fun `saved opinion gets uuid v7 id`() {
        val saved = opinionRepository.save(
            OpinionPersistence(
                owner = UUID.fromString("07070707-0707-0707-0707-070707070707"),
                subject = UUID.fromString("23232323-2323-2323-2323-232323232323"),
                mark = null,
                status = OpinionStatusEnum.DRAFT,
                timestamp = Instant.now(),
            )
        )

        val id = saved.id!!
        assertEquals(7, id.version(), "Expected UUID v7 but got v${id.version()}")
    }
}
