package com.ureka.play4change.auth.adapter.outbound.persistence.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Id val id: String = UUID.randomUUID().toString(),
    @Column(name = "token_hash", nullable = false, unique = true) val tokenHash: String,
    @Column(name = "user_id", nullable = false) val userId: String,
    @Column(name = "family_id", nullable = false) val familyId: String,
    @Column(name = "expires_at", nullable = false) val expiresAt: OffsetDateTime,
    @Column(nullable = false) var used: Boolean = false,
    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(nullable = false, length = 10)
    val role: String = "USER"
)
