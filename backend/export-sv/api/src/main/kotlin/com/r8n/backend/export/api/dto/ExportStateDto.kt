package com.r8n.backend.export.api.dto

import java.time.Instant
import java.util.UUID

data class ExportStateDto(
    val userId: UUID,
    val status: ExportStatus,
    val createdAt: Instant,
    val completedAt: Instant?,
    val estimatedCompletionTime: Instant?,
)

enum class ExportStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
}