package com.r8n.backend.messaging

import com.r8n.backend.messaging.domain.MessageAuthorRoleEnum
import com.r8n.backend.messaging.domain.ThreadTypeEnum
import com.r8n.backend.messaging.persistence.MessagePersistence
import com.r8n.backend.messaging.persistence.ThreadPersistence
import com.r8n.backend.messaging.provider.database.MessageRepository
import com.r8n.backend.messaging.provider.database.ThreadRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
class MessagingApplicationTests {
    private companion object {
        @Container
        @ServiceConnection
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer(DockerImageName.parse("postgres:15"))
                .withDatabaseName("messaging")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("db/init-schema.sql")
    }

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var threadRepository: ThreadRepository

    @Autowired
    lateinit var messageRepository: MessageRepository

    @Test
    fun contextLoads() {
    }

    @Test
    fun `liquibase creates messaging tables with read markers`() {
        assertEquals(
            1,
            count("SELECT COUNT(*) FROM pg_tables WHERE schemaname = 'messaging' AND tablename = 'threads'"),
        )
        assertEquals(
            1,
            count("SELECT COUNT(*) FROM pg_tables WHERE schemaname = 'messaging' AND tablename = 'messages'"),
        )

        assertEquals(
            9,
            count(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'messaging'
                  AND table_name = 'threads'
                  AND column_name IN (
                      'id',
                      'type',
                      'requester_user_id',
                      'recipient_user_id',
                      'created_at',
                      'updated_at',
                      'requester_read_at',
                      'recipient_read_at',
                      'support_read_at'
                  )
                """.trimIndent(),
            ),
        )
        assertEquals(
            1,
            count(
                """
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'messaging'
                  AND tablename = 'threads'
                  AND indexname = 'uq_threads_direct_participants'
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `direct thread uniqueness is enforced regardless of participant order`() {
        val firstUser = UUID.randomUUID()
        val secondUser = UUID.randomUUID()
        val timestamp = Timestamp.from(Instant.parse("2026-01-01T10:00:00Z"))

        insertDirectThread(UUID.randomUUID(), firstUser, secondUser, timestamp)

        assertThrows(DataIntegrityViolationException::class.java) {
            insertDirectThread(UUID.randomUUID(), secondUser, firstUser, timestamp)
        }
    }

    @Test
    fun `messages are removed with their thread`() {
        val threadId = UUID.randomUUID()
        val messageId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val timestamp = Timestamp.from(Instant.parse("2026-01-01T10:00:00Z"))

        jdbcTemplate.update(
            """
            INSERT INTO messaging.threads (
                id,
                type,
                requester_user_id,
                created_at,
                updated_at,
                requester_read_at
            )
            VALUES (?, 'SUPPORT', ?, ?, ?, ?)
            """.trimIndent(),
            threadId,
            userId,
            timestamp,
            timestamp,
            timestamp,
        )
        jdbcTemplate.update(
            """
            INSERT INTO messaging.messages (
                id,
                thread_id,
                author_user_id,
                author_role,
                text,
                created_at
            )
            VALUES (?, ?, ?, 'USER', 'hello support', ?)
            """.trimIndent(),
            messageId,
            threadId,
            userId,
            timestamp,
        )

        jdbcTemplate.update("DELETE FROM messaging.threads WHERE id = ?", threadId)

        assertEquals(
            0,
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM messaging.messages WHERE id = ?",
                Int::class.java,
                messageId,
            ),
        )
    }

    @Test
    fun `repository finds visible threads newest first`() {
        val currentUserId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val hiddenUserId = UUID.randomUUID()
        val olderThread =
            threadRepository.save(
                directThread(
                    requesterUserId = currentUserId,
                    recipientUserId = otherUserId,
                    createdAt = Instant.parse("2026-01-01T09:00:00Z"),
                    updatedAt = Instant.parse("2026-01-01T09:00:00Z"),
                ),
            )
        val newerThread =
            threadRepository.save(
                supportThread(
                    requesterUserId = currentUserId,
                    createdAt = Instant.parse("2026-01-01T10:00:00Z"),
                    updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
                ),
            )
        threadRepository.save(
            directThread(
                requesterUserId = hiddenUserId,
                recipientUserId = otherUserId,
                createdAt = Instant.parse("2026-01-01T11:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T11:00:00Z"),
            ),
        )

        val actual = threadRepository.findVisibleByUserId(currentUserId, PageRequest.of(0, 10)).content

        assertEquals(listOf(newerThread.id, olderThread.id), actual.map { it.id })
    }

    @Test
    fun `repository finds direct thread regardless of participant order`() {
        val firstUserId = UUID.randomUUID()
        val secondUserId = UUID.randomUUID()
        val thread =
            threadRepository.save(
                directThread(
                    requesterUserId = firstUserId,
                    recipientUserId = secondUserId,
                    createdAt = Instant.parse("2026-01-01T10:00:00Z"),
                    updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
                ),
            )

        val actual = threadRepository.findDirectThreadBetweenParticipants(secondUserId, firstUserId)

        assertEquals(thread.id, actual?.id)
    }

    @Test
    fun `repository finds messages by thread in chronological order`() {
        val userId = UUID.randomUUID()
        val thread =
            threadRepository.save(
                supportThread(
                    requesterUserId = userId,
                    createdAt = Instant.parse("2026-01-01T10:00:00Z"),
                    updatedAt = Instant.parse("2026-01-01T10:00:00Z"),
                ),
            )
        val secondMessage =
            messageRepository.save(
                message(
                    threadId = thread.id!!,
                    authorUserId = userId,
                    text = "second",
                    createdAt = Instant.parse("2026-01-01T10:02:00Z"),
                ),
            )
        val firstMessage =
            messageRepository.save(
                message(
                    threadId = thread.id!!,
                    authorUserId = userId,
                    text = "first",
                    createdAt = Instant.parse("2026-01-01T10:01:00Z"),
                ),
            )

        val actual = messageRepository.findByThreadIdOrderByCreatedAtAsc(thread.id!!, PageRequest.of(0, 10)).content

        assertEquals(listOf(firstMessage.id, secondMessage.id), actual.map { it.id })
    }

    private fun insertDirectThread(
        threadId: UUID,
        requesterUserId: UUID,
        recipientUserId: UUID,
        timestamp: Timestamp,
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO messaging.threads (
                id,
                type,
                requester_user_id,
                recipient_user_id,
                created_at,
                updated_at,
                requester_read_at
            )
            VALUES (?, 'DIRECT', ?, ?, ?, ?, ?)
            """.trimIndent(),
            threadId,
            requesterUserId,
            recipientUserId,
            timestamp,
            timestamp,
            timestamp,
        )
    }

    private fun count(sql: String): Int = jdbcTemplate.queryForObject(sql, Int::class.java) ?: 0

    private fun supportThread(
        requesterUserId: UUID,
        createdAt: Instant,
        updatedAt: Instant,
    ) = ThreadPersistence(
        type = ThreadTypeEnum.SUPPORT,
        requesterUserId = requesterUserId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        requesterReadAt = createdAt,
    )

    private fun directThread(
        requesterUserId: UUID,
        recipientUserId: UUID,
        createdAt: Instant,
        updatedAt: Instant,
    ) = ThreadPersistence(
        type = ThreadTypeEnum.DIRECT,
        requesterUserId = requesterUserId,
        recipientUserId = recipientUserId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        requesterReadAt = createdAt,
    )

    private fun message(
        threadId: UUID,
        authorUserId: UUID,
        text: String,
        createdAt: Instant,
    ) = MessagePersistence(
        threadId = threadId,
        authorUserId = authorUserId,
        authorRole = MessageAuthorRoleEnum.USER,
        text = text,
        createdAt = createdAt,
    )
}