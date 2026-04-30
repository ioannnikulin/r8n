package com.r8n.backend.opinions.access.domain

import java.time.Instant
import java.util.UUID

class AccessRequest(
    var id: UUID? = null,
    var listId: UUID,
    var requesterId: UUID,
    var ownerId: UUID, // domain and dto, not persistence
    var status: RequestStatusEnum,
    var createdAt: Instant,
    var updatedAt: Instant,
    var intent: AccessRequestIntent = AccessRequestIntent.NONE,
    var targetListId: UUID? = null,
)
