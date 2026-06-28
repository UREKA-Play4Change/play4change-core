package com.ureka.play4change.auth.domain.model

import java.time.OffsetDateTime

data class RecoveryEmail(
    val id: String,
    val userId: String,
    val email: String,
    val verified: Boolean,
    val tokenHash: String?,
    val tokenExpiresAt: OffsetDateTime?,
    val createdAt: OffsetDateTime
)