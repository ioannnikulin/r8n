package com.r8n.backend.messaging.service

import com.r8n.backend.messaging.domain.MessageAuthorRoleEnum
import com.r8n.backend.messaging.domain.MessagingThread
import com.r8n.backend.messaging.domain.ThreadMessage
import com.r8n.backend.messaging.domain.ThreadTypeEnum
import com.r8n.backend.messaging.persistence.MessagePersistence
import com.r8n.backend.messaging.persistence.ThreadPersistence
import com.r8n.backend.messaging.provider.database.MessageRepository
import com.r8n.backend.messaging.provider.database.ThreadRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class MessagingService(
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository,
) {
    @Transactional(readOnly = true)
    fun getVisibleThreads(
        requesterId: UUID,
        pageable: Pageable,
    ): Page<MessagingThread> =
        threadRepository
            .findVisibleByUserId(requesterId, pageable)
            .map { it.toDomain(requesterId) }

    @Transactional(readOnly = true)
    fun getMessages(
        requesterId: UUID,
        threadId: UUID,
        pageable: Pageable,
    ): Page<ThreadMessage> {
        getVisibleThreadOrThrow(requesterId, threadId)
        return messageRepository.findByThreadIdOrderByCreatedAtAsc(threadId, pageable).map { it.toDomain() }
    }

    @Transactional
    fun createSupportThread(
        requesterId: UUID,
        initialMessage: String,
    ): MessagingThread {
        validateMessage(initialMessage)

        val now = Instant.now()
        val thread =
            threadRepository.save(
                ThreadPersistence(
                    type = ThreadTypeEnum.SUPPORT,
                    requesterUserId = requesterId,
                    createdAt = now,
                    updatedAt = now,
                    requesterReadAt = now,
                ),
            )
        addMessageToThread(thread, requesterId, initialMessage, now)

        return thread.toDomain(requesterId)
    }

    @Transactional
    fun createDirectThread(
        requesterId: UUID,
        recipientUserId: UUID,
        initialMessage: String,
    ): MessagingThread {
        validateMessage(initialMessage)
        if (requesterId == recipientUserId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Direct thread recipient must be another user")
        }

        val thread =
            threadRepository.findDirectThreadBetweenParticipants(requesterId, recipientUserId)
                ?: createDirectThread(requesterId, recipientUserId)
        sendMessage(requesterId, thread.id!!, initialMessage)

        return threadRepository.getReferenceById(thread.id!!).toDomain(requesterId)
    }

    @Transactional
    fun sendMessage(
        requesterId: UUID,
        threadId: UUID,
        text: String,
    ): ThreadMessage {
        validateMessage(text)

        val thread = getVisibleThreadOrThrow(requesterId, threadId)
        val now = Instant.now()
        val message = addMessageToThread(thread, requesterId, text, now)
        markThreadReadForUser(thread, requesterId, now)
        thread.updatedAt = now

        return message.toDomain()
    }

    @Transactional
    fun markRead(
        requesterId: UUID,
        threadId: UUID,
    ): MessagingThread {
        val thread = getVisibleThreadOrThrow(requesterId, threadId)
        markThreadReadForUser(thread, requesterId, Instant.now())
        return thread.toDomain(requesterId)
    }

    private fun createDirectThread(
        requesterId: UUID,
        recipientUserId: UUID,
    ): ThreadPersistence {
        val now = Instant.now()
        return threadRepository.save(
            ThreadPersistence(
                type = ThreadTypeEnum.DIRECT,
                requesterUserId = requesterId,
                recipientUserId = recipientUserId,
                createdAt = now,
                updatedAt = now,
                requesterReadAt = now,
            ),
        )
    }

    private fun addMessageToThread(
        thread: ThreadPersistence,
        requesterId: UUID,
        text: String,
        createdAt: Instant,
    ): MessagePersistence =
        messageRepository.save(
            MessagePersistence(
                threadId = thread.id!!,
                authorUserId = requesterId,
                authorRole = MessageAuthorRoleEnum.USER,
                text = text.trim(),
                createdAt = createdAt,
            ),
        )

    private fun getVisibleThreadOrThrow(
        requesterId: UUID,
        threadId: UUID,
    ): ThreadPersistence {
        val thread =
            threadRepository
                .findById(threadId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (!thread.isVisibleTo(requesterId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return thread
    }

    private fun markThreadReadForUser(
        thread: ThreadPersistence,
        userId: UUID,
        readAt: Instant,
    ) {
        when (userId) {
            thread.requesterUserId -> thread.requesterReadAt = readAt
            thread.recipientUserId -> thread.recipientReadAt = readAt
            else -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
    }

    private fun ThreadPersistence.toDomain(requesterId: UUID): MessagingThread =
        MessagingThread(
            id = id!!,
            type = type,
            requesterUserId = requesterUserId,
            recipientUserId = recipientUserId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            requesterReadAt = requesterReadAt,
            recipientReadAt = recipientReadAt,
            supportReadAt = supportReadAt,
            lastMessage = messageRepository.findFirstByThreadIdOrderByCreatedAtDesc(id!!)?.toDomain(),
            unreadCount = countUnreadMessages(this, requesterId),
        )

    private fun countUnreadMessages(
        thread: ThreadPersistence,
        requesterId: UUID,
    ): Long {
        val threadId = thread.id!!
        val readAt =
            when (requesterId) {
                thread.requesterUserId -> thread.requesterReadAt
                thread.recipientUserId -> thread.recipientReadAt
                else -> null
            }
        return if (readAt == null) {
            messageRepository.countByThreadId(threadId)
        } else {
            messageRepository.countByThreadIdAndCreatedAtAfter(threadId, readAt)
        }
    }

    private fun ThreadPersistence.isVisibleTo(userId: UUID): Boolean =
        requesterUserId == userId || recipientUserId == userId

    private fun MessagePersistence.toDomain(): ThreadMessage =
        ThreadMessage(
            id = id!!,
            threadId = threadId,
            authorUserId = authorUserId,
            authorRole = authorRole,
            text = text,
            createdAt = createdAt,
        )

    private fun validateMessage(text: String) {
        if (text.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Message text must not be blank")
        }
    }
}