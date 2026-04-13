package com.r8n.backend.export.api

import com.r8n.backend.export.api.dto.ExportStateDto
import com.r8n.backend.export.api.dto.UserCompleteDataDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import java.util.UUID

interface ExportApi {
    companion object {
        const val START_PATH = "/export/users/{userId}/start"
        const val STATUS_PATH = "/export/users/{userId}/status"
        const val DOWNLOAD_PATH = "/export/users/{userId}/download"
    }

    @PostMapping(START_PATH)
    fun startGeneratingExportFor(
        @PathVariable("userId") userId: UUID,
    ): ResponseEntity<Void>

    @GetMapping(STATUS_PATH)
    fun checkIfDataIsReady(
        @PathVariable("userId") userId: UUID,
    ): ExportStateDto

    @GetMapping(DOWNLOAD_PATH)
    fun downloadData(
        @PathVariable("userId") userId: UUID,
    ): UserCompleteDataDto
}