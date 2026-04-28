package com.r8n.backend.messaging.domain

import java.time.Instant
import java.util.UUID

data class MessagingThread(
    val id: UUID,
    val type: ThreadTypeEnum,
    val requesterUserId: UUID,
    val recipientUserId: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val requesterReadAt: Instant?,
    val recipientReadAt: Instant?,
    val supportReadAt: Instant?,
    val lastMessage: ThreadMessage?,
    val unreadCount: Long,
)