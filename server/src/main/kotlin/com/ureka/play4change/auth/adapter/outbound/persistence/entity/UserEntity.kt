package com.ureka.play4change.auth.adapter.outbound.persistence.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id val id: String = UUID.randomUUID().toString(),
    @Column(nullable = false, unique = true) val email: String,
    val name: String? = null,
    @Column(nullable = false) val provider: String,
    @Column(name = "provider_id") val providerId: String? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
