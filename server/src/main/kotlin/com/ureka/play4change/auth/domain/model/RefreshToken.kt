package com.ureka.play4change.auth.domain.model

import java.time.OffsetDateTime

data class RefreshToken(
    val id: String,
    val tokenHash: String,
    val userId: String,
    val familyId: String,
    val expiresAt: OffsetDateTime,
    val used: Boolean,
    val createdAt: OffsetDateTime
) {
    fun isExpired(): Boolean = OffsetDateTime.now().isAfter(expiresAt)
    fun isValid(): Boolean = !used && !isExpired()
}
