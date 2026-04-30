package com.r8n.backend.opinions.access.persistence

import com.r8n.backend.opinions.access.domain.AccessRequestIntent
import com.r8n.backend.opinions.access.domain.RequestStatusEnum
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
@Table(schema = "opinions", name = "access_requests")
class AccessRequestPersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null,
//
    @Column(nullable = false)
    var list: UUID,
//
    @Column(nullable = false)
    var requester: UUID,
//
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: RequestStatusEnum,
//
    @Column(nullable = false)
    var createdAt: Instant,
//
    @Column(nullable = false)
    var updatedAt: Instant,
//
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var intent: AccessRequestIntent = AccessRequestIntent.NONE,
//
    @Column(name = "target_list_id")
    var targetListId: UUID? = null,
)
