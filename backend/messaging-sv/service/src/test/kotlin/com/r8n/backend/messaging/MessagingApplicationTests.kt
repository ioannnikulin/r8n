package com.r8n.backend.messaging

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.messaging.api.dto.CreateDirectThreadRequestDto
import com.r8n.backend.messaging.api.dto.CreateSupportThreadRequestDto
import com.r8n.backend.messaging.api.dto.CreateThreadMessageRequestDto
import com.r8n.backend.messaging.api.dto.ThreadTypeEnumDto
import com.r8n.backend.messaging.domain.MessageAuthorRoleEnum
import com.r8n.backend.messaging.domain.ThreadTypeEnum
import com.r8n.backend.messaging.facade.MessagingFacade
import com.r8n.backend.messaging.persistence.MessagePersistence
import com.r8n.backend.messaging.persistence.ThreadPersistence
import com.r8n.backend.messaging.provider.database.MessageRepository
import com.r8n.backend.messaging.provider.database.ThreadRepository
import com.r8n.backend.messaging.service.MessagingService
import com.r8n.backend.users.integration.api.UsersInternalApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.server.ResponseStatusException
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

    @Autowired
    lateinit var messagingService: MessagingService

    @Autowired
    lateinit var messagingFacade: MessagingFacade

    @MockitoBean
    lateinit var usersInternalApi: UsersInternalApi

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

    @Test
    fun `service creates support thread and calculates own unread count`() {
        val requesterId = UUID.randomUUID()

        val actual = messagingService.createSupportThread(requesterId, " hello support ")

        assertEquals(ThreadTypeEnum.SUPPORT, actual.type)
        assertEquals(requesterId, actual.requesterUserId)
        assertEquals("hello support", actual.lastMessage?.text)
        assertEquals(0, actual.unreadCount)
    }

    @Test
    fun `service reuses direct thread and calculates unread count for recipient`() {
        val requesterId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()

        val thread = messagingService.createDirectThread(requesterId, recipientId, "first")
        messagingService.sendMessage(requesterId, thread.id, "second")

        val visibleToRecipient = messagingService.getVisibleThreads(recipientId, PageRequest.of(0, 10)).single()
        assertEquals(thread.id, visibleToRecipient.id)
        assertEquals(2, visibleToRecipient.unreadCount)

        val reused = messagingService.createDirectThread(recipientId, requesterId, "reply")
        assertEquals(thread.id, reused.id)
        assertEquals(thread.id, threadRepository.findDirectThreadBetweenParticipants(requesterId, recipientId)?.id)
    }

    @Test
    fun `service hides messages in threads not visible to requester`() {
        val ownerId = UUID.randomUUID()
        val strangerId = UUID.randomUUID()
        val thread = messagingService.createSupportThread(ownerId, "private")

        assertThrows(ResponseStatusException::class.java) {
            messagingService.getMessages(strangerId, thread.id, PageRequest.of(0, 10))
        }
    }

    @Test
    fun `service marks direct thread read for recipient`() {
        val requesterId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val thread = messagingService.createDirectThread(requesterId, recipientId, "hello")

        assertEquals(1, messagingService.getVisibleThreads(recipientId, PageRequest.of(0, 10)).single().unreadCount)

        val readThread = messagingService.markRead(recipientId, thread.id)

        assertEquals(0, readThread.unreadCount)
        assertEquals(0, messagingService.getVisibleThreads(recipientId, PageRequest.of(0, 10)).single().unreadCount)
    }

    @Test
    fun `facade maps thread summary with username enrichment`() {
        val requesterId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        whenever(usersInternalApi.getUserName(any())).thenReturn("Direct Recipient")

        val actual =
            messagingFacade.createDirectThread(
                requesterId,
                CreateDirectThreadRequestDto(recipientUserId = recipientId, initialMessage = "hello"),
            )

        assertEquals(ThreadTypeEnumDto.DIRECT, actual.type)
        assertEquals(recipientId, actual.participant.userId)
        assertEquals("Direct Recipient", actual.participant.name)
        assertEquals("hello", actual.lastMessagePreview)
        assertEquals(true, actual.lastMessageOwn)
    }

    @Test
    fun `facade maps messages with own flag and author names`() {
        val requesterId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        whenever(usersInternalApi.getUserName(requesterId)).thenReturn("Requester")
        whenever(usersInternalApi.getUserName(recipientId)).thenReturn("Recipient")
        val thread =
            messagingFacade.createDirectThread(
                requesterId,
                CreateDirectThreadRequestDto(recipientUserId = recipientId, initialMessage = "hello"),
            )
        messagingFacade.addThreadMessage(
            recipientId,
            thread.id,
            CreateThreadMessageRequestDto(text = "hi"),
        )

        val actual = messagingFacade.getThreadMessages(requesterId, thread.id, PageRequestDto(0, 10)).items

        assertEquals(listOf("Requester", "Recipient"), actual.map { it.authorName })
        assertEquals(listOf(true, false), actual.map { it.own })
    }

    @Test
    fun `facade maps support participant without user lookup`() {
        val requesterId = UUID.randomUUID()

        val actual =
            messagingFacade.createSupportThread(
                requesterId,
                CreateSupportThreadRequestDto(initialMessage = "support"),
            )

        assertEquals(ThreadTypeEnumDto.SUPPORT, actual.type)
        assertEquals(null, actual.participant.userId)
        assertEquals("R8N Support", actual.participant.name)
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