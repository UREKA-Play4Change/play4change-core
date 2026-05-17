package com.ureka.play4change.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "device_tokens")
class DeviceTokenEntity(

    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "user_id", nullable = false, length = 36)
    val userId: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var token: String,

    @Column(nullable = false, length = 10)
    val platform: String,

    @Column(name = "last_notified_at")
    var lastNotifiedAt: OffsetDateTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
