package com.r8n.backend.export.api

import com.r8n.backend.export.api.dto.ExportStateDto
import com.r8n.backend.export.api.dto.UserCompleteDataDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import java.util.UUID

interface ExportApi {
    @PostMapping("/users/{userId}/start")
    fun startGeneratingExportFor(
        @PathVariable("userId") userId: UUID,
    ): ResponseEntity<Void>

    @GetMapping("/users/{userId}/status")
    fun checkIfDataIsReady(
        @PathVariable("userId") userId: UUID,
    ): ExportStateDto

    @GetMapping("/users/{userId}/download")
    fun downloadData(
        @PathVariable("userId") userId: UUID,
    ): UserCompleteDataDto
}