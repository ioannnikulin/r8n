package com.r8n.backend.users.provider.database

import com.r8n.backend.users.persistence.UserPersistence
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserRepository : JpaRepository<UserPersistence, UUID> {
    @Query("SELECT u FROM UserPersistence u JOIN PIIPersistence p ON u.id = p.userId WHERE p.email = :email")
    fun findByEmail(email: String): UserPersistence?
}
