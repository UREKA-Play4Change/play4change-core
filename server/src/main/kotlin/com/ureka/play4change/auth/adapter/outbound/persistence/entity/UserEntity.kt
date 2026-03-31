package com.ureka.play4change.auth.adapter.outbound.persistence.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false, unique = true)
    val email: String,

    val name: String? = null,

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    val avatarUrl: String? = null,

    @Column(nullable = false, length = 10)
    val role: String = "USER",

    @Column(nullable = false, length = 20)
    val provider: String,

    @Column(name = "provider_id")
    val providerId: String? = null,

    @Column(name = "preferred_language", nullable = false, length = 10)
    val preferredLanguage: String = "en",

    @Column(name = "audience_level", nullable = false, length = 20)
    val audienceLevel: String = "BEGINNER",

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
