package com.r8n.backend.export.api

import com.r8n.backend.export.api.dto.ExportStateDto
import com.r8n.backend.export.api.dto.UserCompleteDataDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping

interface ExportApi {
    companion object {
        const val START_PATH = "/export/start"
        const val STATUS_PATH = "/export/status"
        const val DOWNLOAD_PATH = "/export/download"
    }

    @PostMapping(START_PATH)
    fun startGeneratingExportFor(): ResponseEntity<Void>

    @GetMapping(STATUS_PATH)
    fun checkIfDataIsReady(): ExportStateDto

    @GetMapping(DOWNLOAD_PATH)
    fun downloadData(): UserCompleteDataDto
}