package com.r8n.backend.export.api

import com.r8n.backend.export.api.dto.ExportStateDto
import com.r8n.backend.export.api.dto.UserCompleteDataDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping

@Tag(name = "Data export", description = "Self-service export of the authenticated user's stored data.")
interface ExportApi {
    companion object {
        private const val BASE_PATH = "/api/export"
        const val START_PATH = "$BASE_PATH/start"
        const val STATUS_PATH = "$BASE_PATH/status"
        const val DOWNLOAD_PATH = "$BASE_PATH/download"
    }

    @PostMapping(START_PATH)
    @Operation(
        summary = "Start data export",
        description = "Queues generation of the authenticated user's complete data export and returns immediately.",
    )
    fun startGeneratingExportFor(): ResponseEntity<Void>

    @GetMapping(STATUS_PATH)
    @Operation(
        summary = "Get export status",
        description = "Returns whether the authenticated user's generated data export is ready to download.",
    )
    fun checkIfDataIsReady(): ExportStateDto

    @GetMapping(DOWNLOAD_PATH)
    @Operation(
        summary = "Download data export",
        description = "Returns the authenticated user's generated data export when it is ready.",
    )
    fun downloadData(): UserCompleteDataDto
}
