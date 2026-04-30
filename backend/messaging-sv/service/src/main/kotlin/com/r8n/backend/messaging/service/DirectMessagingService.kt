package com.r8n.backend.messaging.service

import com.r8n.backend.messaging.persistence.ConversationParticipantPersistence
import com.r8n.backend.messaging.persistence.ConversationParticipantRoleEnumPersistence
import com.r8n.backend.messaging.persistence.ConversationPersistence
import com.r8n.backend.messaging.persistence.ConversationTypeEnumPersistence
import com.r8n.backend.messaging.persistence.MessagePersistence
import com.r8n.backend.messaging.provider.database.ConversationParticipantRepository
import com.r8n.backend.messaging.provider.database.ConversationRepository
import com.r8n.backend.messaging.provider.database.MessageRepository
import com.r8n.backend.users.api.dto.UserStatusEnumDto
import com.r8n.backend.users.integration.api.UsersInternalApi
import com.r8n.backend.users.integration.api.dto.UserDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class DirectMessagingService(
    private val conversationRepository: ConversationRepository,
    private val conversationParticipantRepository: ConversationParticipantRepository,
    private val messageRepository: MessageRepository,
    private val usersInternalApi: UsersInternalApi,
) {
    fun listVisibleConversations(
        actor: DirectActor,
        pageable: Pageable,
    ): Page<ConversationPersistence> =
        conversationRepository.findVisibleByTypeAndParticipantOrderByLastMessageAtDesc(
            type = ConversationTypeEnumPersistence.DIRECT,
            userId = actor.userId,
            pageable = pageable,
        )

    fun countUnreadMessages(actor: DirectActor): Long =
        messageRepository.countUnreadMessagesForParticipant(actor.userId)

    @Transactional
    fun createConversation(
        actor: DirectActor,
        recipientUserId: UUID,
        initialMessageText: String,
    ): DirectConversationWithMessages {
        validateMessageText(initialMessageText, "Initial message")
        if (actor.userId == recipientUserId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create a direct conversation with yourself")
        }

        val sender = getActiveUserOrThrow(actor.userId)
        getActiveUserOrThrow(recipientUserId)

        conversationRepository.lockDirectConversationPair(actor.userId.directPairKey(recipientUserId))

        val existingConversation =
            conversationRepository.findDirectConversationBetweenUsers(
                type = ConversationTypeEnumPersistence.DIRECT,
                firstUserId = actor.userId,
                secondUserId = recipientUserId,
            )
        if (existingConversation != null) {
            val message = addConversationMessage(actor, requireNotNull(existingConversation.id), initialMessageText)
            return DirectConversationWithMessages(
                conversation = existingConversation,
                participants =
                    conversationParticipantRepository.findAllByConversationId(
                        requireNotNull(existingConversation.id),
                    ),
                messages = listOf(message),
            )
        }

        val now = Instant.now()
        val conversation =
            conversationRepository.save(
                ConversationPersistence(
                    type = ConversationTypeEnumPersistence.DIRECT,
                    createdByUserId = actor.userId,
                    createdAt = now,
                    lastMessageAt = now,
                ),
            )
        val conversationId = requireNotNull(conversation.id)
        val participants =
            conversationParticipantRepository
                .saveAll(
                    listOf(
                        ConversationParticipantPersistence(
                            conversationId = conversationId,
                            userId = actor.userId,
                            participantRole = ConversationParticipantRoleEnumPersistence.MEMBER,
                            joinedAt = now,
                            lastReadAt = now,
                        ),
                        ConversationParticipantPersistence(
                            conversationId = conversationId,
                            userId = recipientUserId,
                            participantRole = ConversationParticipantRoleEnumPersistence.MEMBER,
                            joinedAt = now,
                        ),
                    ),
                ).toList()
        val message =
            messageRepository.save(
                MessagePersistence(
                    conversationId = conversationId,
                    authorUserId = actor.userId,
                    authorDisplayNameSnapshot = sender.name,
                    authorRoleSnapshot = actor.role,
                    text = initialMessageText.trim(),
                    createdAt = now,
                ),
            )

        return DirectConversationWithMessages(
            conversation = conversation,
            participants = participants,
            messages = listOf(message),
        )
    }

    fun getConversationMessages(
        actor: DirectActor,
        conversationId: UUID,
        pageable: Pageable,
    ): Page<MessagePersistence> {
        findConversationVisibleForActor(actor, conversationId)
        return messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversationId, pageable)
    }

    @Transactional
    fun addConversationMessage(
        actor: DirectActor,
        conversationId: UUID,
        text: String,
    ): MessagePersistence {
        validateMessageText(text, "Message text")
        findConversationVisibleForActor(actor, conversationId)
        val author = getActiveUserOrThrow(actor.userId)
        val now = Instant.now()
        val message =
            messageRepository.save(
                MessagePersistence(
                    conversationId = conversationId,
                    authorUserId = actor.userId,
                    authorDisplayNameSnapshot = author.name,
                    authorRoleSnapshot = actor.role,
                    text = text.trim(),
                    createdAt = now,
                ),
            )
        val conversation =
            conversationRepository.findById(conversationId).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found")
            }
        conversation.lastMessageAt = now
        conversationRepository.save(conversation)
        return message
    }

    @Transactional
    fun markConversationAsRead(
        actor: DirectActor,
        conversationId: UUID,
    ) {
        val participant =
            conversationParticipantRepository.findByConversationIdAndUserId(conversationId, actor.userId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found")
        participant.lastReadAt = Instant.now()
        conversationParticipantRepository.save(participant)
    }

    private fun findConversationVisibleForActor(
        actor: DirectActor,
        conversationId: UUID,
    ): ConversationPersistence {
        val conversation =
            conversationRepository.findById(conversationId).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found")
            }
        if (conversation.type != ConversationTypeEnumPersistence.DIRECT) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found")
        }
        if (!conversationParticipantRepository.existsByConversationIdAndUserId(conversationId, actor.userId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found")
        }
        return conversation
    }

    private fun getActiveUserOrThrow(userId: UUID): UserDto {
        val user =
            try {
                usersInternalApi.getUser(userId)
            } catch (_: ResponseStatusException) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
            } catch (_: RestClientResponseException) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
            }
        if (user.status != UserStatusEnumDto.ACTIVE) {
            throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "User is not active")
        }
        return user
    }

    private fun validateMessageText(
        text: String,
        fieldName: String,
    ) {
        if (text.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "$fieldName cannot be blank")
        }
    }

    private fun UUID.directPairKey(otherUserId: UUID): String =
        listOf(this, otherUserId)
            .sorted()
            .joinToString(separator = ":")
}

data class DirectConversationWithMessages(
    val conversation: ConversationPersistence,
    val participants: List<ConversationParticipantPersistence>,
    val messages: List<MessagePersistence>,
)
