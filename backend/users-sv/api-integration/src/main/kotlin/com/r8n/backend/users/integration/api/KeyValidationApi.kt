package com.r8n.backend.users.integration.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

interface KeyValidationApi {
    // for gateway's reactive WebClient connection, not for interservice RestClient
    companion object {
        const val VALIDATE_API_KEY_PATH = "/api/internal/users/api-keys/validate/{key}"
    }

    @GetMapping(VALIDATE_API_KEY_PATH)
    fun validateApiKey(
        @PathVariable key: String,
    ): UUID
}
