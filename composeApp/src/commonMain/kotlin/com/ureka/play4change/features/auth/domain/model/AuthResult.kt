package com.ureka.play4change.features.auth.domain.model

import com.ureka.play4change.auth.AuthTokens

/**
 * Client-side auth result. Wraps the server AuthTokens.
 * userId extracted from JWT subject on the client side after parsing.
 * Backward-compat: token property returns accessToken for existing mock usage.
 */
data class AuthResult(
    val userId: String,
    val tokens: AuthTokens
) {
    /** Legacy accessor — returns accessToken. Keeps existing mock code compiling. */
    val token: String get() = tokens.accessToken
}

enum class SocialProvider { GOOGLE, FACEBOOK }
