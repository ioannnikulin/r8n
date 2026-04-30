package com.r8n.backend.opinions.api.opinions.dto

import java.time.Instant
import java.util.UUID

data class OpinionRowDto(
    val opinionId: UUID,
    val owner: UUID,
    val ownerName: String,
    val subjective: List<String>,
    val objective: List<String>,
    val mark: Double?,
    val status: OpinionStatusEnumDto,
    val timestamp: Instant,
    val weight: Double,
)

data class OpinionSummaryDto(
    val subject: UUID,
    val subjectName: String,
    val referentName: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val ownMark: Double?,
    val componentMark: Double?,
    val opinions: List<OpinionRowDto>,
)
