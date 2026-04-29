package com.r8n.backend.opinions.api.opinions.dto

import java.time.Instant
import java.util.UUID

data class ModerationDecisionDto(
    val id: UUID,
    val opinionId: UUID,
    val subjectName: String,
    val ownerName: String,
    val moderatorId: UUID,
    val moderatorName: String,
    val action: ModerationDecisionActionDto,
    val previousStatus: OpinionStatusEnumDto,
    val newStatus: OpinionStatusEnumDto,
    val reason: String?,
    val createdAt: Instant,
)
