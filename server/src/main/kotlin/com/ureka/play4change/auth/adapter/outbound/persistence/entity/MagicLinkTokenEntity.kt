package com.ureka.play4change.auth.adapter.outbound.persistence.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "magic_link_tokens")
class MagicLinkTokenEntity(
    @Id val id: String = UUID.randomUUID().toString(),
    @Column(nullable = false, unique = true) val token: String,
    @Column(nullable = false) val email: String,
    @Column(name = "expires_at", nullable = false) val expiresAt: OffsetDateTime,
    @Column(nullable = false) val used: Boolean = false,
    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
