package com.r8n.backend.opinions.api.lists.dto

import java.util.UUID

data class OpinionListNameAndOwnerDto(
    val listId: UUID,
    val listName: String,
    val owner: UUID,
    val ownerName: String,
)
