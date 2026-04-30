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
    const val SUPPORT = "ROLE_SUPPORT"
    const val IS_SUPPORT = "hasRole('$SUPPORT')"
    const val IS_USER_OR_SUPPORT = "hasRole('$USER') or hasRole('$SUPPORT')"
    const val IS_EXPLICIT_USER_OR_MODERATOR_OR_SUPPORT_OR_ADMIN =
        "authentication.authorities.?[authority == '$USER' or authority == '$MODERATOR' or authority == '$SUPPORT' or authority == '$ADMIN'].size() > 0"
    const val SERVICE = "ROLE_SERVICE"
    const val IS_SERVICE = "hasRole('$SERVICE')"
    const val IS_USER_OR_SERVICE = "hasRole('$USER') or hasRole('$SERVICE')"
}
