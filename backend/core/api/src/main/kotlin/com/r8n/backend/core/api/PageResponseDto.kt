package com.r8n.backend.core.api

data class PageResponseDto<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val size: Int,
)
