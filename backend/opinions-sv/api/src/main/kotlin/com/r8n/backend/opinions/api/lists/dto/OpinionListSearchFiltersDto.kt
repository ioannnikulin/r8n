package com.r8n.backend.opinions.api.lists.dto

import java.time.Instant
import java.util.UUID

data class OpinionListSearchFiltersDto(
    val nameSubstring: String? = null,
    val authorId: UUID? = null,
    val authorNameSubstring: String? = null,
    val containsLocationSubstring: String? = null,
    val someOpinionsYoungerThan: Instant? = null,
    val containsSubjectSubstring: String? = null,
    val findThisTextInAnyOfTheAbove: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusInMeters: Double? = null,
)
