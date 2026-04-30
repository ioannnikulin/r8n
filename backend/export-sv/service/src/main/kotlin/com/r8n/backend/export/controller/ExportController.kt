package com.r8n.backend.export.controller

import com.r8n.backend.export.api.ExportApi
import com.r8n.backend.export.api.dto.ExportStateDto
import com.r8n.backend.export.api.dto.UserCompleteDataDto
import com.r8n.backend.export.facade.DataExportFacade
import com.r8n.backend.security.Authority
import com.r8n.backend.security.CurrentUserIdentifier.getCurrentUserId
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class ExportController(
    private val dataExportFacade: DataExportFacade,
) : ExportApi {
    @PreAuthorize(Authority.IS_USER)
    override fun startGeneratingExportFor(): ResponseEntity<Void> {
        val userId = getCurrentUserId()
        dataExportFacade.startExport(userId)
        return ResponseEntity.accepted().build()
    }

    @PreAuthorize(Authority.IS_USER)
    override fun checkIfDataIsReady(): ExportStateDto {
        val userId = getCurrentUserId()
        return dataExportFacade.getExportStatus(userId)
    }

    @PreAuthorize(Authority.IS_USER)
    override fun downloadData(): UserCompleteDataDto {
        val userId = getCurrentUserId()
        return dataExportFacade.getExportData(userId)
    }
}
