package com.r8n.backend.opinions.api.lists.dto

import com.r8n.backend.opinions.api.opinions.dto.OpinionSummaryDto
import java.util.UUID

data class OpinionListDto(
    val id: UUID,
    val listName: String,
    val owner: UUID,
    val ownerName: String,
    val opinionSummaries: List<OpinionSummaryDto>,
    val privacy: OpinionListPrivacyEnumDto,
)
