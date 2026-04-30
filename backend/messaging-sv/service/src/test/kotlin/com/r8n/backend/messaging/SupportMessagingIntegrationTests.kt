package com.r8n.backend.messaging

import com.r8n.backend.messaging.api.MessagingApi.Companion.DIRECT_CONVERSATIONS_PATH
import com.r8n.backend.messaging.api.MessagingApi.Companion.SUPPORT_THREADS_PATH
import com.r8n.backend.messaging.api.MessagingApi.Companion.SUPPORT_THREAD_PATH
import com.r8n.backend.messaging.api.MessagingApi.Companion.UNREAD_COUNT_PATH
import com.r8n.backend.messaging.api.dto.messaging.SUPPORT_MESSAGE_TEXT_MAX_LENGTH
import com.r8n.backend.messaging.persistence.ConversationParticipantPersistence
import com.r8n.backend.messaging.persistence.ConversationParticipantRoleEnumPersistence
import com.r8n.backend.messaging.persistence.ConversationPersistence
import com.r8n.backend.messaging.persistence.ConversationTypeEnumPersistence
import com.r8n.backend.messaging.persistence.MessageAuthorRoleEnumPersistence
import com.r8n.backend.messaging.persistence.MessagePersistence
import com.r8n.backend.messaging.provider.database.ConversationParticipantRepository
import com.r8n.backend.messaging.provider.database.ConversationRepository
import com.r8n.backend.messaging.provider.database.MessageRepository
import com.r8n.backend.messaging.provider.database.SupportMessageRepository
import com.r8n.backend.messaging.provider.database.SupportThreadRepository
import com.r8n.backend.users.api.dto.UserStatusEnumDto
import com.r8n.backend.users.integration.api.UsersInternalApi
import com.r8n.backend.users.integration.api.dto.UserDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SupportMessagingIntegrationTests {
    private companion object {
        @Suppress("unused") // used to store test database container
        @Container
        @ServiceConnection
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer(DockerImageName.parse("postgres:15"))
                .withDatabaseName("messaging")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("db/init-schema.sql")

        val userAId: UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val userBId: UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val supportId: UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        val adminId: UUID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")
        val moderatorId: UUID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var supportThreadRepository: SupportThreadRepository

    @Autowired
    lateinit var supportMessageRepository: SupportMessageRepository

    @Autowired
    lateinit var conversationRepository: ConversationRepository

    @Autowired
    lateinit var conversationParticipantRepository: ConversationParticipantRepository

    @Autowired
    lateinit var messageRepository: MessageRepository

    @MockitoBean
    lateinit var usersInternalApi: UsersInternalApi

    @BeforeEach
    fun setUp() {
        messageRepository.deleteAll()
        conversationParticipantRepository.deleteAll()
        conversationRepository.deleteAll()
        supportMessageRepository.deleteAll()
        supportThreadRepository.deleteAll()
        whenever(usersInternalApi.getUserName(eq(userAId))).thenReturn("User A")
        whenever(usersInternalApi.getUserName(eq(userBId))).thenReturn("User B")
        whenever(usersInternalApi.getUserName(eq(supportId))).thenReturn("Support Agent")
        whenever(usersInternalApi.getUserName(eq(adminId))).thenReturn("Admin Agent")
        whenever(usersInternalApi.getUserName(eq(moderatorId))).thenReturn("Moderator User")
        whenever(usersInternalApi.getUser(eq(userAId))).thenReturn(userDto(userAId, "User A"))
        whenever(usersInternalApi.getUser(eq(userBId))).thenReturn(userDto(userBId, "User B"))
        whenever(usersInternalApi.getUser(eq(supportId))).thenReturn(userDto(supportId, "Support Agent"))
        whenever(usersInternalApi.getUser(eq(adminId))).thenReturn(userDto(adminId, "Admin Agent"))
        whenever(usersInternalApi.getUser(eq(moderatorId))).thenReturn(userDto(moderatorId, "Moderator User"))
    }

    @Test
    fun `user can create thread and read its messages`() {
        val threadId = createThread(userAId, "USER", "Need help with a review dispute")

        mockMvc
            .perform(
                get(messagesPath(threadId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(userAId.toString()).roles("USER")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].threadId").value(threadId.toString()))
            .andExpect(jsonPath("$.items[0].authorUserId").value(userAId.toString()))
            .andExpect(jsonPath("$.items[0].authorDisplayName").value("User A"))
            .andExpect(jsonPath("$.items[0].authorRole").value("USER"))
    }

    @Test
    fun `user cannot read another user thread`() {
        val threadId = createThread(userAId, "USER", "My private support thread")

        mockMvc
            .perform(
                get(messagesPath(threadId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(userBId.toString()).roles("USER")),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `support can read another user thread`() {
        val threadId = createThread(userAId, "USER", "Need a support response")
        addMessage(supportId, "SUPPORT", threadId, "Support response")

        mockMvc
            .perform(
                get(messagesPath(threadId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(supportId.toString()).roles("SUPPORT")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[1].authorDisplayName").value("Support Agent"))
            .andExpect(jsonPath("$.items[1].authorRole").value("SUPPORT"))
    }

    @Test
    fun `support creates own support thread as requester`() {
        val threadId = createThread(supportId, "SUPPORT", "Support user needs private support")

        mockMvc
            .perform(
                get(SUPPORT_THREADS_PATH)
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(supportId.toString()).roles("SUPPORT")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(threadId.toString()))
            .andExpect(jsonPath("$.items[0].viewerRole").value("REQUESTER"))

        mockMvc
            .perform(
                get(messagesPath(threadId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(supportId.toString()).roles("SUPPORT")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].authorRole").value("USER"))
    }

    @Test
    fun `support writes to own support thread as requester`() {
        val threadId = createThread(supportId, "SUPPORT", "Support user needs private support")

        mockMvc
            .perform(
                post(messagesPath(threadId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text":"Requester follow-up"}""")
                    .with(user(supportId.toString()).roles("SUPPORT"))
                    .with(csrf()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.authorUserId").value(supportId.toString()))
            .andExpect(jsonPath("$.authorRole").value("USER"))
    }

    @Test
    fun `admin can read and respond to another user thread as support`() {
        val threadId = createThread(userAId, "USER", "Need admin support")

        mockMvc
            .perform(
                post(messagesPath(threadId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text":"Admin support response"}""")
                    .with(user(adminId.toString()).roles("ADMIN"))
                    .with(csrf()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.authorUserId").value(adminId.toString()))
            .andExpect(jsonPath("$.authorRole").value("SUPPORT"))

        mockMvc
            .perform(
                get(messagesPath(threadId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(adminId.toString()).roles("ADMIN")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[1].authorRole").value("SUPPORT"))
    }

    @Test
    fun `admin creates own support thread as requester`() {
        val threadId = createThread(adminId, "ADMIN", "Admin needs private support")

        mockMvc
            .perform(
                get(SUPPORT_THREADS_PATH)
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(adminId.toString()).roles("ADMIN")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(threadId.toString()))
            .andExpect(jsonPath("$.items[0].viewerRole").value("REQUESTER"))

        mockMvc
            .perform(
                get(messagesPath(threadId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(adminId.toString()).roles("ADMIN")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].authorRole").value("USER"))
    }

    @Test
    fun `moderator can use own support threads as regular user`() {
        val threadId = createThread(moderatorId, "MODERATOR", "Moderator needs user-side support")

        mockMvc
            .perform(
                post(messagesPath(threadId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text":"Moderator follow-up"}""")
                    .with(user(moderatorId.toString()).roles("MODERATOR"))
                    .with(csrf()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.authorUserId").value(moderatorId.toString()))
            .andExpect(jsonPath("$.authorRole").value("USER"))

        mockMvc
            .perform(
                get(messagesPath(threadId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(moderatorId.toString()).roles("MODERATOR")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[1].authorRole").value("USER"))
    }

    @Test
    fun `moderator cannot access another user support thread`() {
        val threadId = createThread(userAId, "USER", "Private support thread")

        mockMvc
            .perform(
                get(messagesPath(threadId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(moderatorId.toString()).roles("MODERATOR")),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `posting blank message returns bad request`() {
        val threadId = createThread(userAId, "USER", "Need clarification")

        mockMvc
            .perform(
                post(messagesPath(threadId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text":"   "}""")
                    .with(user(userAId.toString()).roles("USER"))
                    .with(csrf()),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `creating thread with oversized initial message returns bad request`() {
        val oversizedMessage = "a".repeat(SUPPORT_MESSAGE_TEXT_MAX_LENGTH + 1)

        mockMvc
            .perform(
                post(SUPPORT_THREADS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("initialMessage" to oversizedMessage)))
                    .with(user(userAId.toString()).roles("USER"))
                    .with(csrf()),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `posting oversized message returns bad request`() {
        val threadId = createThread(userAId, "USER", "Need clarification")
        val oversizedMessage = "a".repeat(SUPPORT_MESSAGE_TEXT_MAX_LENGTH + 1)

        mockMvc
            .perform(
                post(messagesPath(threadId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("text" to oversizedMessage)))
                    .with(user(userAId.toString()).roles("USER"))
                    .with(csrf()),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `missing thread returns not found`() {
        mockMvc
            .perform(
                get(messagesPath(UUID.randomUUID()))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(supportId.toString()).roles("SUPPORT")),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `support sees all threads in list while user sees only own`() {
        createThread(userAId, "USER", "Thread from user A")
        createThread(userBId, "USER", "Thread from user B")

        mockMvc
            .perform(
                get(SUPPORT_THREADS_PATH)
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(userAId.toString()).roles("USER")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].viewerRole").value("REQUESTER"))

        mockMvc
            .perform(
                get(SUPPORT_THREADS_PATH)
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(supportId.toString()).roles("SUPPORT")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].viewerRole").value("SUPPORT"))
            .andExpect(jsonPath("$.items[1].viewerRole").value("SUPPORT"))
    }

    @Test
    fun `admin sees all support threads in list`() {
        createThread(userAId, "USER", "Thread from user A")
        createThread(userBId, "USER", "Thread from user B")

        mockMvc
            .perform(
                get(SUPPORT_THREADS_PATH)
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(adminId.toString()).roles("ADMIN")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
    }

    @Test
    fun `thread summaries respect requested page size`() {
        createThread(userAId, "USER", "Thread one")
        createThread(userBId, "USER", "Thread two")

        mockMvc
            .perform(
                get(SUPPORT_THREADS_PATH)
                    .param("page", "0")
                    .param("size", "1")
                    .with(user(supportId.toString()).roles("SUPPORT")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.size").value(1))
    }

    @Test
    fun `thread summaries are ordered by latest message date`() {
        val olderThreadId = createThread(userAId, "USER", "Thread one")
        createThread(userBId, "USER", "Thread two")
        addMessage(userAId, "USER", olderThreadId, "Follow-up makes this thread latest")

        mockMvc
            .perform(
                get(SUPPORT_THREADS_PATH)
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(supportId.toString()).roles("SUPPORT")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(olderThreadId.toString()))
            .andExpect(jsonPath("$.items[0].lastMessageText").value("Follow-up makes this thread latest"))
    }

    @Test
    fun `creating support thread again appends to existing requester support chat`() {
        val threadId = createThread(userAId, "USER", "First support message")

        mockMvc
            .perform(
                post(SUPPORT_THREADS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"initialMessage":"Second support message"}""")
                    .with(user(userAId.toString()).roles("USER"))
                    .with(csrf()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(threadId.toString()))
            .andExpect(jsonPath("$.lastMessageText").value("Second support message"))

        mockMvc
            .perform(
                get(messagesPath(threadId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(userAId.toString()).roles("USER")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
    }

    @Test
    fun `support read by one support user clears unread count for support team`() {
        val threadId = createThread(userAId, "USER", "Unread for support")

        mockMvc
            .perform(
                get(SUPPORT_THREADS_PATH)
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(supportId.toString()).roles("SUPPORT")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(threadId.toString()))
            .andExpect(jsonPath("$.items[0].unreadCount").value(1))

        mockMvc
            .perform(
                get(messagesPath(threadId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(supportId.toString()).roles("SUPPORT")),
            ).andExpect(status().isOk)

        mockMvc
            .perform(
                get(SUPPORT_THREADS_PATH)
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(adminId.toString()).roles("ADMIN")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(threadId.toString()))
            .andExpect(jsonPath("$.items[0].unreadCount").value(0))

        val messages =
            supportMessageRepository.findAllByThreadIdOrderByCreatedAtAsc(
                threadId,
                org.springframework.data.domain.Pageable
                    .unpaged(),
            )
        assertEquals(supportId, messages.content.single().readBySupportUserId)
    }

    @Test
    fun `requester reading own support chat does not clear support unread count`() {
        val threadId = createThread(userAId, "USER", "Still unread for support")

        mockMvc
            .perform(
                get(messagesPath(threadId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(userAId.toString()).roles("USER")),
            ).andExpect(status().isOk)

        mockMvc
            .perform(
                get(SUPPORT_THREADS_PATH)
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(supportId.toString()).roles("SUPPORT")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(threadId.toString()))
            .andExpect(jsonPath("$.items[0].unreadCount").value(1))
    }

    @Test
    fun `support replies are unread for requester until requester reads thread`() {
        val threadId = createThread(userAId, "USER", "Need a requester-visible reply")
        addMessage(supportId, "SUPPORT", threadId, "Support reply for requester")

        mockMvc
            .perform(
                get(SUPPORT_THREADS_PATH)
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(userAId.toString()).roles("USER")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(threadId.toString()))
            .andExpect(jsonPath("$.items[0].unreadCount").value(1))

        mockMvc
            .perform(
                get(UNREAD_COUNT_PATH)
                    .with(user(userAId.toString()).roles("USER")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(1))

        mockMvc
            .perform(
                get(messagesPath(threadId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(userAId.toString()).roles("USER")),
            ).andExpect(status().isOk)

        mockMvc
            .perform(
                get(SUPPORT_THREADS_PATH)
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(userAId.toString()).roles("USER")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(threadId.toString()))
            .andExpect(jsonPath("$.items[0].unreadCount").value(0))

        mockMvc
            .perform(
                get(UNREAD_COUNT_PATH)
                    .with(user(userAId.toString()).roles("USER")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(0))
    }

    @Test
    fun `unread count endpoint returns aggregate count across messaging surfaces`() {
        createThread(userAId, "USER", "Unread support request")
        createDirectConversation(userAId, supportId, "Unread direct message")

        mockMvc
            .perform(
                get(UNREAD_COUNT_PATH)
                    .with(user(supportId.toString()).roles("SUPPORT")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(2))
    }

    @Test
    fun `thread messages respect requested page size`() {
        val threadId = createThread(userAId, "USER", "Message one")
        addMessage(userAId, "USER", threadId, "Message two")

        mockMvc
            .perform(
                get(messagesPath(threadId))
                    .param("page", "0")
                    .param("size", "1")
                    .with(user(userAId.toString()).roles("USER")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.size").value(1))
    }

    @Test
    fun `owner can delete their thread and its messages`() {
        val threadId = createThread(userAId, "USER", "Please help")
        addMessage(userAId, "USER", threadId, "More context")

        mockMvc
            .perform(
                delete(threadPath(threadId))
                    .with(user(userAId.toString()).roles("USER"))
                    .with(csrf()),
            ).andExpect(status().isNoContent)

        assert(!supportThreadRepository.existsById(threadId))
        assert(
            supportMessageRepository
                .findAllByThreadIdOrderByCreatedAtAsc(
                    threadId,
                    org.springframework.data.domain.Pageable
                        .unpaged(),
                ).isEmpty,
        )
    }

    @Test
    fun `support can delete any thread`() {
        val threadId = createThread(userAId, "USER", "Please help")

        mockMvc
            .perform(
                delete(threadPath(threadId))
                    .with(user(supportId.toString()).roles("SUPPORT"))
                    .with(csrf()),
            ).andExpect(status().isNoContent)

        assert(!supportThreadRepository.existsById(threadId))
    }

    @Test
    fun `user cannot delete another user's thread`() {
        val threadId = createThread(userAId, "USER", "Please help")

        mockMvc
            .perform(
                delete(threadPath(threadId))
                    .with(user(userBId.toString()).roles("USER"))
                    .with(csrf()),
            ).andExpect(status().isForbidden)

        assert(supportThreadRepository.existsById(threadId))
    }

    @Test
    fun `shared messaging model persists direct conversation participants and messages`() {
        val now = Instant.parse("2026-04-29T12:00:00Z")
        conversationRepository.save(
            ConversationPersistence(
                type = ConversationTypeEnumPersistence.SUPPORT,
                createdByUserId = supportId,
                createdAt = now.minusSeconds(60),
                lastMessageAt = now.minusSeconds(60),
            ),
        )
        val conversation =
            conversationRepository.save(
                ConversationPersistence(
                    type = ConversationTypeEnumPersistence.DIRECT,
                    createdByUserId = userAId,
                    createdAt = now,
                    lastMessageAt = now,
                ),
            )
        val conversationId = requireNotNull(conversation.id)

        conversationParticipantRepository.saveAll(
            listOf(
                ConversationParticipantPersistence(
                    conversationId = conversationId,
                    userId = userAId,
                    participantRole = ConversationParticipantRoleEnumPersistence.MEMBER,
                    joinedAt = now,
                    lastReadAt = now,
                ),
                ConversationParticipantPersistence(
                    conversationId = conversationId,
                    userId = userBId,
                    participantRole = ConversationParticipantRoleEnumPersistence.MEMBER,
                    joinedAt = now,
                ),
            ),
        )
        messageRepository.save(
            MessagePersistence(
                conversationId = conversationId,
                authorUserId = userAId,
                authorDisplayNameSnapshot = "User A",
                authorRoleSnapshot = MessageAuthorRoleEnumPersistence.USER,
                text = "Hello from direct messaging.",
                createdAt = now,
            ),
        )

        assertTrue(conversationParticipantRepository.existsByConversationIdAndUserId(conversationId, userAId))
        assertTrue(conversationParticipantRepository.existsByConversationIdAndUserId(conversationId, userBId))
        assertEquals(2, conversationParticipantRepository.findAllByConversationId(conversationId).size)

        val messages =
            messageRepository.findAllByConversationIdOrderByCreatedAtAsc(
                conversationId,
                org.springframework.data.domain.Pageable
                    .unpaged(),
            )
        assertEquals(1, messages.totalElements)
        assertEquals("User A", messages.content.single().authorDisplayNameSnapshot)
        assertEquals(MessageAuthorRoleEnumPersistence.USER, messages.content.single().authorRoleSnapshot)

        assertEquals(
            1,
            conversationRepository
                .findAllByTypeOrderByLastMessageAtDesc(
                    ConversationTypeEnumPersistence.SUPPORT,
                    org.springframework.data.domain.Pageable
                        .unpaged(),
                ).totalElements,
        )
        assertEquals(
            conversationId,
            conversationRepository
                .findAllByTypeOrderByLastMessageAtDesc(
                    ConversationTypeEnumPersistence.DIRECT,
                    org.springframework.data.domain.Pageable
                        .unpaged(),
                ).content
                .single()
                .id,
        )
    }

    @Test
    fun `active user can create direct conversation with another active user`() {
        val result =
            mockMvc
                .perform(
                    post(DIRECT_CONVERSATIONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "recipientUserId" to userBId.toString(),
                                    "initialMessage" to "Hello User B",
                                ),
                            ),
                        ).with(user(userAId.toString()).roles("USER"))
                        .with(csrf()),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.participantUserId").value(userBId.toString()))
                .andExpect(jsonPath("$.participantDisplayName").value("User B"))
                .andExpect(jsonPath("$.lastMessageText").value("Hello User B"))
                .andReturn()

        val conversationId = UUID.fromString(objectMapper.readTree(result.response.contentAsString)["id"].asString())

        mockMvc
            .perform(
                get(directMessagesPath(conversationId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(userAId.toString()).roles("USER")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].authorUserId").value(userAId.toString()))
            .andExpect(jsonPath("$.items[0].authorDisplayName").value("User A"))
            .andExpect(jsonPath("$.items[0].authorRole").value("USER"))
            .andExpect(jsonPath("$.items[0].text").value("Hello User B"))

        mockMvc
            .perform(
                get(DIRECT_CONVERSATIONS_PATH)
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(userBId.toString()).roles("USER")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(conversationId.toString()))
            .andExpect(jsonPath("$.items[0].participantUserId").value(userAId.toString()))
            .andExpect(jsonPath("$.items[0].participantDisplayName").value("User A"))
    }

    @Test
    fun `direct conversation participant can respond`() {
        val conversationId = createDirectConversation(userAId, userBId, "Hello User B")

        mockMvc
            .perform(
                post(directMessagesPath(conversationId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text":"Hello User A"}""")
                    .with(user(userBId.toString()).roles("USER"))
                    .with(csrf()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.authorUserId").value(userBId.toString()))
            .andExpect(jsonPath("$.authorDisplayName").value("User B"))
            .andExpect(jsonPath("$.text").value("Hello User A"))

        mockMvc
            .perform(
                get(directMessagesPath(conversationId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(userAId.toString()).roles("USER")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[1].text").value("Hello User A"))
    }

    @Test
    fun `creating duplicate direct conversation appends message to existing conversation`() {
        val conversationId = createDirectConversation(userAId, userBId, "First")

        mockMvc
            .perform(
                post(DIRECT_CONVERSATIONS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "recipientUserId" to userBId.toString(),
                                "initialMessage" to "Second",
                            ),
                        ),
                    ).with(user(userAId.toString()).roles("USER"))
                    .with(csrf()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(conversationId.toString()))
            .andExpect(jsonPath("$.lastMessageText").value("Second"))

        mockMvc
            .perform(
                get(directMessagesPath(conversationId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(userAId.toString()).roles("USER")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
    }

    @Test
    fun `concurrent direct conversation creation reuses one conversation for a user pair`() {
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        val tasks =
            listOf("First concurrent message", "Second concurrent message").map { message ->
                Callable {
                    start.await(5, TimeUnit.SECONDS)
                    mockMvc
                        .perform(
                            post(DIRECT_CONVERSATIONS_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                    objectMapper.writeValueAsString(
                                        mapOf(
                                            "recipientUserId" to userBId.toString(),
                                            "initialMessage" to message,
                                        ),
                                    ),
                                ).with(user(userAId.toString()).roles("USER"))
                                .with(csrf()),
                        ).andExpect(status().isOk)
                        .andReturn()
                }
            }

        val conversationIds =
            try {
                val futures = tasks.map { executor.submit(it) }
                start.countDown()
                futures.map { future ->
                    UUID.fromString(
                        objectMapper
                            .readTree(
                                future.get(10, TimeUnit.SECONDS).response.contentAsString,
                            )["id"]
                            .asString(),
                    )
                }
            } finally {
                executor.shutdown()
            }

        assertEquals(1, conversationIds.toSet().size)
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))

        val conversationId = conversationIds.toSet().single()
        assertEquals(
            1,
            conversationRepository
                .findAllByTypeOrderByLastMessageAtDesc(
                    ConversationTypeEnumPersistence.DIRECT,
                    org.springframework.data.domain.Pageable
                        .unpaged(),
                ).totalElements,
        )

        mockMvc
            .perform(
                get(directMessagesPath(conversationId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(userAId.toString()).roles("USER")),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
    }

    @Test
    fun `user cannot create direct conversation with self`() {
        mockMvc
            .perform(
                post(DIRECT_CONVERSATIONS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "recipientUserId" to userAId.toString(),
                                "initialMessage" to "Hello myself",
                            ),
                        ),
                    ).with(user(userAId.toString()).roles("USER"))
                    .with(csrf()),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `user cannot create direct conversation with inactive user`() {
        whenever(usersInternalApi.getUser(eq(userBId)))
            .thenReturn(userDto(userBId, "User B", UserStatusEnumDto.SUSPENDED))

        mockMvc
            .perform(
                post(DIRECT_CONVERSATIONS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "recipientUserId" to userBId.toString(),
                                "initialMessage" to "Hello User B",
                            ),
                        ),
                    ).with(user(userAId.toString()).roles("USER"))
                    .with(csrf()),
            ).andExpect(status().`is`(422))
    }

    @Test
    fun `non participant cannot read or send direct messages`() {
        val conversationId = createDirectConversation(userAId, userBId, "Hello User B")

        mockMvc
            .perform(
                get(directMessagesPath(conversationId))
                    .param("page", "0")
                    .param("size", "20")
                    .with(user(moderatorId.toString()).roles("MODERATOR")),
            ).andExpect(status().isNotFound)

        mockMvc
            .perform(
                post(directMessagesPath(conversationId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text":"I should not be here"}""")
                    .with(user(moderatorId.toString()).roles("MODERATOR"))
                    .with(csrf()),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `deleting non-existent thread returns 404`() {
        mockMvc
            .perform(
                delete(threadPath(UUID.randomUUID()))
                    .with(user(userAId.toString()).roles("USER"))
                    .with(csrf()),
            ).andExpect(status().isNotFound)
    }

    private fun createThread(
        authorId: UUID,
        role: String,
        initialMessage: String,
    ): UUID {
        val result =
            mockMvc
                .perform(
                    post(SUPPORT_THREADS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"initialMessage":"$initialMessage"}""")
                        .with(user(authorId.toString()).roles(role))
                        .with(csrf()),
                ).andExpect(status().isOk)
                .andReturn()

        val body = objectMapper.readTree(result.response.contentAsString)
        return UUID.fromString(body["id"].asString())
    }

    private fun addMessage(
        authorId: UUID,
        role: String,
        threadId: UUID,
        text: String,
    ) {
        mockMvc
            .perform(
                post(messagesPath(threadId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text":"$text"}""")
                    .with(user(authorId.toString()).roles(role))
                    .with(csrf()),
            ).andExpect(status().isOk)
    }

    private fun createDirectConversation(
        senderId: UUID,
        recipientId: UUID,
        initialMessage: String,
    ): UUID {
        val result =
            mockMvc
                .perform(
                    post(DIRECT_CONVERSATIONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                mapOf(
                                    "recipientUserId" to recipientId.toString(),
                                    "initialMessage" to initialMessage,
                                ),
                            ),
                        ).with(user(senderId.toString()).roles("USER"))
                        .with(csrf()),
                ).andExpect(status().isOk)
                .andReturn()

        return UUID.fromString(objectMapper.readTree(result.response.contentAsString)["id"].asString())
    }

    private fun userDto(
        id: UUID,
        name: String,
        status: UserStatusEnumDto = UserStatusEnumDto.ACTIVE,
    ) = UserDto(
        id = id,
        name = name,
        email = "$id@r8n.test",
        status = status,
        statusTimestamp = Instant.parse("2024-01-01T12:00:00Z"),
        consents = emptyList(),
    )

    private fun threadPath(threadId: UUID): String = SUPPORT_THREAD_PATH.replace("{threadId}", threadId.toString())

    private fun messagesPath(threadId: UUID): String = "/api/messaging/support/threads/$threadId/messages"

    private fun directMessagesPath(conversationId: UUID): String = "$DIRECT_CONVERSATIONS_PATH/$conversationId/messages"
}
