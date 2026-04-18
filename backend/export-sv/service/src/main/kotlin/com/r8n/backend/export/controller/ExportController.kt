package com.r8n.backend.export.controller

import com.r8n.backend.export.api.ExportApi
import com.r8n.backend.export.api.dto.ExportStateDto
import com.r8n.backend.export.api.dto.UserCompleteDataDto
import com.r8n.backend.export.facade.DataExportFacade
import com.r8n.backend.security.Authority
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ExportController(
    private val dataExportFacade: DataExportFacade,
) : ExportApi {
    private fun getAuthenticatedUserId(): UUID {
        val auth =
            SecurityContextHolder.getContext().authentication
                ?: throw IllegalStateException("Not authenticated")
        return UUID.fromString(auth.name)
    }

    @PreAuthorize(Authority.IS_USER)
    override fun startGeneratingExportFor(): ResponseEntity<Void> {
        val userId = getAuthenticatedUserId()
        dataExportFacade.startExport(userId)
        return ResponseEntity.accepted().build()
    }

    @PreAuthorize(Authority.IS_USER)
    override fun checkIfDataIsReady(): ExportStateDto {
        val userId = getAuthenticatedUserId()
        return dataExportFacade.getExportStatus(userId)
    }

    @PreAuthorize(Authority.IS_USER)
    override fun downloadData(): UserCompleteDataDto {
        val userId = getAuthenticatedUserId()
        return dataExportFacade.getExportData(userId)
    }
}