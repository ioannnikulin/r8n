package com.r8n.backend.core.api

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class PageRequestDto(
    @field:Min(0)
    val page: Int,
    @field:Min(1)
    val size: Int,
    @field:Valid
    val sort: List<SortDto> = emptyList(),
)

data class SortDto(
    @field:NotBlank
    val property: String,
    val direction: SortDirection = SortDirection.ASC,
)

enum class SortDirection {
    ASC,
    DESC,
}
