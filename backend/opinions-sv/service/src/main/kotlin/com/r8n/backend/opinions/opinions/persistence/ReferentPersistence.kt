package com.r8n.backend.opinions.opinions.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(schema = "opinions", name = "referents")
class ReferentPersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null,
//
    @Column(nullable = false)
    var name: String,
//
    @Column(nullable = true)
    var address: String?,
//
    @Column(nullable = true)
    var latitude: Double?,
//
    @Column(nullable = true)
    var longitude: Double?,
//
    @Column(nullable = false)
    var referentGroup: UUID,
)
