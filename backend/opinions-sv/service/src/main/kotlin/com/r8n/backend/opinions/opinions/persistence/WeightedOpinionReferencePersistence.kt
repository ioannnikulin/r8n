package com.r8n.backend.opinions.opinions.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(schema = "opinions", name = "weighted_opinion_references")
class WeightedOpinionReferencePersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null,
//
    @Column(nullable = false)
    var parentOpinion: UUID,
//
    @Column(nullable = false)
    var childOpinion: UUID,
//
    @Column(nullable = false)
    var weight: Double,
)
