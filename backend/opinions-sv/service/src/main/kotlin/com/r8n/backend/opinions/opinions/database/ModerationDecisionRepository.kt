package com.r8n.backend.opinions.opinions.database

import com.r8n.backend.opinions.opinions.persistence.ModerationDecisionPersistence
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ModerationDecisionRepository : JpaRepository<ModerationDecisionPersistence, UUID> {
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<ModerationDecisionPersistence>
}
