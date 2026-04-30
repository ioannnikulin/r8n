package com.r8n.backend.users.provider.database

import com.r8n.backend.users.persistence.RefreshTokenPersistence
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshTokenPersistence, UUID> {
    fun findByTokenId(tokenId: UUID): RefreshTokenPersistence?

    fun findByUserId(userId: UUID): List<RefreshTokenPersistence>

    fun deleteByUserId(userId: UUID)

    fun deleteByExpiresAtBefore(expiry: Instant)
}
