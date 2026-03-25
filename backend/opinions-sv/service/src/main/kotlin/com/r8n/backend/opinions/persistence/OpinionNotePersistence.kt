package com.r8n.backend.opinions.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(schema = "opinions", name = "opinion_note")
class OpinionNotePersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    val id: UUID,

    @Column(nullable = false)
    val opinionId: UUID,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val type: OpinionNoteTypeEnum,

    @Column(nullable = false)
    val description: String,
)

enum class OpinionNoteTypeEnum {
    OBJECTIVE,
    SUBJECTIVE,
}