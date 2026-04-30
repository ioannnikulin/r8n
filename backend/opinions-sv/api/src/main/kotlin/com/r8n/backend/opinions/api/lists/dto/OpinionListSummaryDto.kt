package com.r8n.backend.opinions.api.lists.dto

import java.util.UUID

data class OpinionListSummaryDto(
    val listId: UUID,
    val listName: String,
    val owner: UUID,
    val ownerName: String,
    val opinionsCount: Long,
    val grantedAccessCount: Int,
    val privacy: OpinionListPrivacyEnumDto,
)
