package com.r8n.backend.users.provider.database

import com.r8n.backend.users.domain.UserStatusEnum
import com.r8n.backend.users.persistence.UserPersistence
import org.springframework.data.domain.Pageable
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

    @Query(
        """
        SELECT
            account.id AS id,
            pii.name AS name,
            account.lastSeenAt AS lastSeenAt
        FROM UserPersistence account
        JOIN PIIPersistence pii ON account.id = pii.userId
        WHERE account.status = :status
          AND account.id <> :excludedUserId
          AND LOWER(pii.name) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY LOWER(pii.name), account.id
        """,
    )
    fun searchByNameAndStatus(
        query: String,
        status: UserStatusEnum,
        excludedUserId: UUID,
        pageable: Pageable,
    ): List<UserSearchProjection>
}

interface UserSearchProjection {
    val id: UUID
    val name: String
    val lastSeenAt: Instant?
}
