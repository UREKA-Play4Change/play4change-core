package com.ureka.play4change.auth.adapter.outbound.persistence.entity

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "recovery_emails")
class RecoveryEmailEntity(
    @Id
    val id: String,

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val verified: Boolean = false,

    @Column(name = "token_hash")
    val tokenHash: String? = null,

    @Column(name = "token_expires_at")
    val tokenExpiresAt: OffsetDateTime? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)