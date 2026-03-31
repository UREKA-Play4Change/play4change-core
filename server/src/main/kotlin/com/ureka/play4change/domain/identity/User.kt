package com.ureka.play4change.domain.identity

import java.time.OffsetDateTime

data class User(
    val id: String,
    val email: String,
    val name: String?,
    val avatarUrl: String?,
    val role: UserRole,
    val provider: AuthProvider,
    val providerId: String?,
    val preferredLanguage: String,
    val audienceLevel: String,
    val createdAt: OffsetDateTime
)
