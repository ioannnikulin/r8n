package com.r8n.backend.opinions.lists.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(schema = "opinions", name = "opinions_to_lists")
class OpinionsToOpinionListsPersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null,
//
    @Column(nullable = false)
    var opinionList: UUID,
//
    @Column(nullable = false)
    var opinion: UUID,
//
    @Column(nullable = false)
    var weight: Double,
) {
    fun copy(
        id: UUID? = this.id,
        opinionList: UUID = this.opinionList,
        opinion: UUID = this.opinion,
        weight: Double = this.weight,
    ) = OpinionsToOpinionListsPersistence(id, opinionList, opinion, weight)
}
