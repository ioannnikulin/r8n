package com.r8n.backend.users.provider.database

import com.r8n.backend.users.persistence.UserPersistence
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

interface UserRepository : JpaRepository<UserPersistence, UUID> {
    @Query("SELECT u FROM UserPersistence u JOIN PIIPersistence p ON u.id = p.userId WHERE p.email = :email")
    fun findByEmail(email: String): UserPersistence?

    @Query(
        """
        SELECT u
        FROM UserPersistence u
        JOIN PIIPersistence p ON u.id = p.userId
        WHERE LOWER(p.email) = :normalizedEmail
        """,
    )
    fun findByNormalizedEmail(normalizedEmail: String): UserPersistence?

    @Modifying
    @Transactional
    @Query("UPDATE UserPersistence u SET u.passwordHash = :passwordHash WHERE u.id = :userId")
    fun updatePassword(
        userId: UUID,
        passwordHash: String?,
    )

    @Modifying
    @Transactional
    @Query("UPDATE UserPersistence u SET u.lastSeenAt = :lastSeenAt WHERE u.id = :userId")
    fun updateLastSeenAt(
        userId: UUID,
        lastSeenAt: Instant,
    )
}
