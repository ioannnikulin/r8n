package com.r8n.backend.opinions.api.opinions.dto

import java.time.Instant
import java.util.UUID

data class OpinionDto(
    val id: UUID,
    val owner: UUID,
    val ownerName: String,
    val subject: UUID,
    val subjectName: String,
    val subjective: List<String>,
    val objective: List<String>,
    val mark: Double?,
    val componentMark: Double?,
    val components: List<WeightedOpinionReferenceDto>,
    val status: OpinionStatusEnumDto,
    val timestamp: Instant,
)

data class WeightedOpinionReferenceDto(
    val id: UUID,
    val opinion: UUID,
    val weight: Double,
)
