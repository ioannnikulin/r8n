package com.r8n.backend.users.provider.database

import com.r8n.backend.users.persistence.UserRoleAssignmentPersistence
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRoleAssignmentRepository : JpaRepository<UserRoleAssignmentPersistence, UUID> {
    fun findAllByUser(user: UUID): List<UserRoleAssignmentPersistence>
}