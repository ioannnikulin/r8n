package com.r8n.backend.security

import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

object CurrentUserIdentifier {
    fun getCurrentUserId(): UUID {
        val auth =
            SecurityContextHolder.getContext().authentication ?: throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
            )
        return UUID.fromString(auth.name)
    }
}
