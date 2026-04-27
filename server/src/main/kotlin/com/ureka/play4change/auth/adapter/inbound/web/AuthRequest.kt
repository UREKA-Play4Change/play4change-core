package com.ureka.play4change.auth.adapter.inbound.web

import com.ureka.play4change.auth.domain.model.AuthProvider

data class MagicLinkRequest(val email: String)

data class MagicLinkVerifyRequest(val token: String)

/**
 * Accepts both `idToken` (legacy) and `credential` (web frontend / Google One-Tap).
 * Exactly one must be non-null; validation happens at the controller before calling the use-case.
 */
data class OAuthRequest(
    val provider: AuthProvider,
    val idToken: String? = null,
    val credential: String? = null
) {
    fun resolvedToken(): String =
        credential ?: idToken ?: throw IllegalArgumentException("OAuthRequest requires idToken or credential")
}

data class RefreshRequest(val refreshToken: String)
