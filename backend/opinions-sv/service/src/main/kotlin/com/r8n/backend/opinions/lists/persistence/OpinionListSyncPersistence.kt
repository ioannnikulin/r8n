package com.r8n.backend.opinions.lists.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(schema = "opinions", name = "opinion_lists_syncs")
class OpinionListSyncPersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null,
//
    @Column(nullable = false)
    var sourceList: UUID,
//
    @Column(nullable = false)
    var destinationList: UUID,
//
    @Column(nullable = false)
    var weight: Double = 1.0,
)
