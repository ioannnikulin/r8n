package com.r8n.backend.users.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TokenCleanupScheduler(
    private val tokenService: TokenService,
) {
    private companion object {
        private val log = LoggerFactory.getLogger(TokenCleanupScheduler::class.java)
    }

    /**
     * Periodically clean up expired refresh tokens from the database.
     * Runs once a day.
     */
    @Scheduled(cron = "0 0 3 * * *") // Every day at 3 AM
    fun cleanupExpiredTokens() {
        log.info("Starting cleanup of expired refresh tokens")
        try {
            tokenService.cleanupExpiredTokens()
            log.info("Finished cleanup of expired refresh tokens")
        } catch (e: Exception) {
            log.error("Failed to clean up expired refresh tokens", e)
        }
    }
}
