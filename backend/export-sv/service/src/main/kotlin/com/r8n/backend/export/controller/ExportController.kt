package com.r8n.backend.export.controller

import com.r8n.backend.export.api.ExportApi
import com.r8n.backend.export.api.dto.ExportStateDto
import com.r8n.backend.export.api.dto.UserCompleteDataDto
import com.r8n.backend.export.facade.DataExportFacade
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ExportController(
    private val dataExportFacade: DataExportFacade
) : ExportApi {

    override fun startGeneratingExportFor(userId: UUID): ResponseEntity<Void> {
        // Verify the user is accessing their own data
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("Not authenticated")
        val authenticatedUserId = UUID.fromString(auth.name)

        if (authenticatedUserId != userId) {
            throw SecurityException("Cannot start export for another user")
        }

        dataExportFacade.startExport(userId)
        return ResponseEntity.accepted().build()
    }

    override fun checkIfDataIsReady(userId: UUID): ExportStateDto {
        // Verify the user is accessing their own data
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("Not authenticated")
        val authenticatedUserId = UUID.fromString(auth.name)

        if (authenticatedUserId != userId) {
            throw SecurityException("Cannot check export for another user")
        }

        return dataExportFacade.getExportStatus(userId)
    }

    override fun downloadData(userId: UUID): UserCompleteDataDto {
        // Verify the user is accessing their own data
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("Not authenticated")
        val authenticatedUserId = UUID.fromString(auth.name)

        if (authenticatedUserId != userId) {
            throw SecurityException("Cannot download data for another user")
        }

        return dataExportFacade.getExportData(userId)
    }
}