package com.r8n.backend.opinions.access.database

import com.r8n.backend.opinions.access.domain.RequestStatusEnum
import com.r8n.backend.opinions.access.persistence.AccessRequestPersistence
import com.r8n.backend.opinions.lists.database.OpinionListRepository
import com.r8n.backend.opinions.lists.domain.OpinionListPrivacyEnum
import com.r8n.backend.opinions.lists.persistence.OpinionListPersistence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.domain.Pageable
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.UUID

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
class AccessRequestRepositoryTest {
    private companion object {
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
    lateinit var repository: AccessRequestRepository

    @Autowired
    lateinit var opinionListRepository: OpinionListRepository

    @Test
    fun `findAllByFilters filters by ownerId correctly`() {
        val owner1 = UUID.randomUUID()
        val owner2 = UUID.randomUUID()
        val requester = UUID.randomUUID()

        val list1 =
            opinionListRepository.save(
                OpinionListPersistence(
                    owner = owner1,
                    name = "List 1",
                    privacy = OpinionListPrivacyEnum.SEARCHABLE,
                ),
            )
        val list2 =
            opinionListRepository.save(
                OpinionListPersistence(
                    owner = owner2,
                    name = "List 2",
                    privacy = OpinionListPrivacyEnum.SEARCHABLE,
                ),
            )

        repository.save(
            AccessRequestPersistence(
                list = list1.id!!,
                requester = requester,
                status = RequestStatusEnum.SENT,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )
        repository.save(
            AccessRequestPersistence(
                list = list2.id!!,
                requester = requester,
                status = RequestStatusEnum.SENT,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

        val owner1Requests = repository.findAllByFilters(null, null, owner1, null, Pageable.unpaged())
        assertEquals(1, owner1Requests.totalElements)
        assertEquals(list1.id, owner1Requests.content[0].list)

        val owner2Requests = repository.findAllByFilters(null, null, owner2, null, Pageable.unpaged())
        assertEquals(1, owner2Requests.totalElements)
        assertEquals(list2.id, owner2Requests.content[0].list)

        val allRequests = repository.findAllByFilters(null, null, null, null, Pageable.unpaged())
        // Might have more from other tests if not using @DataJpaTest or similar, but here it's @SpringBootTest
        // Let's at least check we have at least 2.
        assert(allRequests.totalElements >= 2)
    }

    @Test
    fun `findAllByFilters filters by updatedAt when since is provided`() {
        val owner = UUID.randomUUID()
        val requester = UUID.randomUUID()
        val oldUpdatedAt = Instant.parse("2026-01-01T10:00:00Z")
        val since = Instant.parse("2026-01-02T10:00:00Z")
        val newUpdatedAt = Instant.parse("2026-01-03T10:00:00Z")

        val list =
            opinionListRepository.save(
                OpinionListPersistence(
                    owner = owner,
                    name = "List with access requests",
                    privacy = OpinionListPrivacyEnum.SEARCHABLE,
                ),
            )

        repository.save(
            AccessRequestPersistence(
                list = list.id!!,
                requester = requester,
                status = RequestStatusEnum.SENT,
                createdAt = oldUpdatedAt,
                updatedAt = oldUpdatedAt,
            ),
        )
        val newerRequest =
            repository.save(
                AccessRequestPersistence(
                    list = list.id!!,
                    requester = requester,
                    status = RequestStatusEnum.ACCEPTED,
                    createdAt = oldUpdatedAt,
                    updatedAt = newUpdatedAt,
                ),
            )

        val requests = repository.findAllByFiltersUpdatedSince(list.id, null, owner, since, null, Pageable.unpaged())

        assertEquals(1, requests.totalElements)
        assertEquals(newerRequest.id, requests.content.single().id)
    }
}
