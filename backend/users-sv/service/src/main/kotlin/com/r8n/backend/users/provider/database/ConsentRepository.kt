package com.r8n.backend.users.provider.database

import com.r8n.backend.users.persistence.ConsentPersistence
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ConsentRepository : JpaRepository<ConsentPersistence, UUID> {
    fun findAllByUserId(userId: UUID): List<ConsentPersistence>
}
