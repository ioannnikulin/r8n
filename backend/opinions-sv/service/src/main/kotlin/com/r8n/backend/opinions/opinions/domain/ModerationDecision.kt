package com.r8n.backend.opinions.opinions.domain

import java.time.Instant
import java.util.UUID

data class ModerationDecision(
    val id: UUID,
    val opinionId: UUID,
    val subjectName: String,
    val owner: UUID,
    val moderator: UUID,
    val action: ModerationDecisionAction,
    val previousStatus: OpinionStatusEnum,
    val newStatus: OpinionStatusEnum,
    val reason: String?,
    val createdAt: Instant,
)
