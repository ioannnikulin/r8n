package com.r8n.backend.messaging.service

import com.r8n.backend.messaging.persistence.SupportParticipantRoleEnumPersistence
import java.util.UUID

data class SupportActor(
    val userId: UUID,
    val role: SupportParticipantRoleEnumPersistence,
)
