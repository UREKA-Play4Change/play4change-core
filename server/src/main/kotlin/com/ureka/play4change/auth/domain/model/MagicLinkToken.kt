package com.ureka.play4change.auth.domain.model

import java.time.OffsetDateTime

data class MagicLinkToken(
    val id: String,
    val token: String,
    val email: String,
    val expiresAt: OffsetDateTime,
    val used: Boolean,
    val createdAt: OffsetDateTime
) {
    fun isExpired(): Boolean = OffsetDateTime.now().isAfter(expiresAt)
    fun isValid(): Boolean = !used && !isExpired()
}
