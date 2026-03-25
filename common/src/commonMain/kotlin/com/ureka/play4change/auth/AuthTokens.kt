package com.ureka.play4change.auth

import kotlinx.serialization.Serializable

/**
 * Returned by the server on successful authentication.
 * accessToken: short-lived JWT (15 min) — kept in memory only, never persisted.
 * refreshToken: long-lived opaque token (7 days) — stored in secure persistent storage.
 * expiresIn: access token lifetime in seconds.
 */
@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)
