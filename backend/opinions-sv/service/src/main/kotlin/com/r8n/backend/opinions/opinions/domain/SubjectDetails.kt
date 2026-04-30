package com.r8n.backend.opinions.opinions.domain

import java.util.UUID

data class SubjectDetails(
    val id: UUID,
    val name: String,
    val primaryReferent: SubjectReferent,
    val alternativeReferents: List<SubjectReferent>,
)

data class SubjectReferent(
    val id: UUID,
    val name: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
)
