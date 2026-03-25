package com.r8n.backend.opinions.persistence

import com.r8n.backend.opinions.domain.OpinionStatusEnum
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
@Table(schema = "opinions", name = "opinions")
class OpinionPersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null,

    @Column(nullable = false)
    var owner: UUID,

    @Column(nullable = false)
    var subject: UUID,

    @Column(nullable = true)
    var mark: Double?,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OpinionStatusEnum,

    @Column(nullable = false)
    var timestamp: Instant,
)