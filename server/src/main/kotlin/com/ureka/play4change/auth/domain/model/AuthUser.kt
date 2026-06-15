package com.ureka.play4change.auth.domain.model

import com.ureka.play4change.auth.AuthProvider
import java.time.OffsetDateTime

/**
 * Auth-bounded-context view of a user — minimal set of fields needed to issue
 * tokens and validate credentials. Not the same as [com.ureka.play4change.domain.identity.User],
 * which is the full application-domain entity.
 */
data class AuthUser(
    val id: String,
    val email: String,
    val name: String?,
    val provider: AuthProvider,
    val providerId: String?,
    val createdAt: OffsetDateTime,
    val role: String = "USER"
)
