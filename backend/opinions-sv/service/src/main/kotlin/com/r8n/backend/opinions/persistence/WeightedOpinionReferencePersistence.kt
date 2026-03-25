package com.r8n.backend.opinions.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(schema = "opinions", name = "weighted_opinion_reference")
class WeightedOpinionReferencePersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    val id: UUID,

    @Column(nullable = false)
    val parentOpinion: UUID,

    @Column(nullable = false)
    val childOpinion: UUID,

    @Column(nullable = false)
    val weight: Double,
)