package com.r8n.backend.messaging.persistence

import com.r8n.backend.messaging.domain.ThreadTypeEnum
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
@Table(schema = "messaging", name = "threads")
class ThreadPersistence(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null,
//
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var type: ThreadTypeEnum,
//
    @Column(nullable = false)
    var requesterUserId: UUID,
//
    @Column
    var recipientUserId: UUID? = null,
//
    @Column(nullable = false)
    var createdAt: Instant,
//
    @Column(nullable = false)
    var updatedAt: Instant,
//
    @Column
    var requesterReadAt: Instant? = null,
//
    @Column
    var recipientReadAt: Instant? = null,
//
    @Column
    var supportReadAt: Instant? = null,
)