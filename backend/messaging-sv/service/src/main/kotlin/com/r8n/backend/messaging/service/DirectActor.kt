package com.r8n.backend.messaging.service

import com.r8n.backend.messaging.persistence.MessageAuthorRoleEnumPersistence
import java.util.UUID

data class DirectActor(
    val userId: UUID,
    val role: MessageAuthorRoleEnumPersistence,
)
