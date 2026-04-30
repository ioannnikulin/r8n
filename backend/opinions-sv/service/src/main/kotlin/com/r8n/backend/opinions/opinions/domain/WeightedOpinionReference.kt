package com.r8n.backend.opinions.opinions.domain

import java.util.UUID

data class WeightedOpinionReference(
    /*
     as of now, it's used for both including opinions in lists (suggests the same opinion subject)
     and (not implemented in frontend) linking opinions to other opinions (subjects can be different)
     */
    val id: UUID,
    val opinion: UUID,
    val weight: Double,
)
