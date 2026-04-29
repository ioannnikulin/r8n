package com.r8n.backend.users.provider.database

import com.r8n.backend.users.persistence.PIIPersistence
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface PIIRepository : JpaRepository<PIIPersistence, UUID> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE PIIPersistence p
        SET p.name = :name,
            p.about = :about,
            p.location = :location
        WHERE p.userId = :userId
        """,
    )
    fun updatePublicProfile(
        userId: UUID,
        name: String,
        about: String?,
        location: String?,
    ): Int

    fun findAllByNameContainingIgnoreCase(nameSubstring: String): List<PIIPersistence>
}
