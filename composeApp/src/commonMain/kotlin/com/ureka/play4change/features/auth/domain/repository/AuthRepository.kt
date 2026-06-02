package com.ureka.play4change.features.auth.domain.repository

import com.ureka.play4change.features.auth.domain.model.AuthResult
import com.ureka.play4change.features.auth.domain.model.MagicLinkResult

interface AuthRepository {
    /** Step 1 of magic link: send the email. Returns success/failure of sending. */
    suspend fun sendMagicLink(email: String): MagicLinkResult

    /** Step 2 of magic link: exchange the token from the link for auth tokens. */
    suspend fun verifyMagicLink(token: String): AuthResult?

    /** Silent token refresh using stored refresh token. */
    suspend fun refresh(refreshToken: String): AuthResult?

    /** Register new user (name + email magic link flow). */
    suspend fun register(name: String, email: String): Boolean

    /** Revoke the stored refresh token and clear local token storage. */
    suspend fun logout()
}
