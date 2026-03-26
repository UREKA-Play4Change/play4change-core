package com.ureka.play4change.auth.domain.model

import java.time.OffsetDateTime

data class User(
    val id: String,
    val email: String,
    val name: String?,
    val provider: AuthProvider,
    val providerId: String?,
    val createdAt: OffsetDateTime
)
