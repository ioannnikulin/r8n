package com.r8n.backend.opinions.api.opinions.dto

import java.util.UUID

data class OpinionSubjectDto(
    val id: UUID,
    val name: String,
    val primaryReferent: ReferentDto?,
    val alternativeReferents: List<ReferentDto>,
)
