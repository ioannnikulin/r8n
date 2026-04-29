package com.r8n.backend.opinions.lists.domain

import java.util.UUID

data class OpinionListInfo(
    val id: UUID,
    val name: String,
    val owner: UUID,
    val privacy: OpinionListPrivacyEnum,
    val opinionsCount: Long,
    val grantedAccessCount: Int,
    val ownerName: String? = null,
)
