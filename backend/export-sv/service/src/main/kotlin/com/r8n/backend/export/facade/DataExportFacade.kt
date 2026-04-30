package com.r8n.backend.export.facade

import com.r8n.backend.export.api.dto.ExportStateDto
import com.r8n.backend.export.api.dto.ExportStatus
import com.r8n.backend.export.api.dto.UserCompleteDataDto
import com.r8n.backend.export.service.DataExportService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class DataExportFacade(
    private val dataExportService: DataExportService,
) {
    fun startExport(userId: UUID) {
        dataExportService.startExport(userId)
    }

    fun getExportStatus(userId: UUID): ExportStateDto {
        val job =
            dataExportService.getJob(userId)
                ?: throw ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No export job found for user $userId",
                )

        return ExportStateDto(
            userId = job.userId,
            status = job.status.toApiStatus(),
            createdAt = job.createdAt,
            completedAt = job.completedAt,
            estimatedCompletionTime = null,
        )
    }

    fun getExportData(userId: UUID): UserCompleteDataDto {
        val job =
            dataExportService.getJob(userId)
                ?: throw ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No export job found for user $userId",
                )

        if (job.status != DataExportService.ExportJobStatus.COMPLETED) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Export not completed yet. Status: ${job.status}",
            )
        }

        return job.result
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Completed export has no data for user $userId")
    }

    private fun DataExportService.ExportJobStatus.toApiStatus(): ExportStatus =
        when (this) {
            DataExportService.ExportJobStatus.PENDING -> ExportStatus.PENDING
            DataExportService.ExportJobStatus.IN_PROGRESS -> ExportStatus.IN_PROGRESS
            DataExportService.ExportJobStatus.COMPLETED -> ExportStatus.COMPLETED
            DataExportService.ExportJobStatus.FAILED -> ExportStatus.FAILED
            DataExportService.ExportJobStatus.CANCELLED -> ExportStatus.FAILED
        }
}
