package com.r8n.backend.security

object Authority {
    const val ADMIN = "ROLE_ADMIN"
    const val IS_ADMIN = "hasRole('$ADMIN')"
    const val MODERATOR = "ROLE_MODERATOR"
    const val IS_MODERATOR = "hasRole('$MODERATOR')"
    const val AI_MODERATOR = "ROLE_AI_MODERATOR"
    const val IS_AI_MODERATOR = "hasRole('$AI_MODERATOR')"
    const val USER = "ROLE_USER"
    const val IS_USER = "hasRole('$USER')"
    const val SERVICE = "ROLE_SERVICE"
    const val IS_SERVICE = "hasRole('$SERVICE')"
}