package com.r8n.backend.opinions.opinions.persistence

import com.r8n.backend.opinions.opinions.domain.ModerationDecisionAction
import com.r8n.backend.opinions.opinions.domain.OpinionStatusEnum
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(schema = "opinions", name = "moderation_decisions")
class ModerationDecisionPersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null,
//
    @Column(nullable = false)
    var opinion: UUID,
//
    @Column(nullable = false)
    var moderator: UUID,
//
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var action: ModerationDecisionAction,
//
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var previousStatus: OpinionStatusEnum,
//
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var newStatus: OpinionStatusEnum,
//
    @Column(nullable = true)
    var reason: String?,
//
    @Column(nullable = false)
    var createdAt: Instant,
)
