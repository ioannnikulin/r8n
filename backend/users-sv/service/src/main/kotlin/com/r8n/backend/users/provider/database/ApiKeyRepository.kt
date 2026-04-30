package com.r8n.backend.users.provider.database

import com.r8n.backend.users.persistence.ApiKeyPersistence
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ApiKeyRepository : JpaRepository<ApiKeyPersistence, UUID> {
    fun findByUserIdAndRevokedFalse(userId: UUID): List<ApiKeyPersistence>

    fun findByKeyIdentifier(keyIdentifier: String): ApiKeyPersistence?

    fun findByKeyHash(keyHash: String): ApiKeyPersistence?
}
