package com.ureka.play4change.features.auth.data.mock

import com.ureka.play4change.auth.AuthTokens
import com.ureka.play4change.features.auth.domain.model.AuthResult
import com.ureka.play4change.features.auth.domain.model.MagicLinkResult
import com.ureka.play4change.features.auth.domain.model.SocialProvider
import com.ureka.play4change.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.delay

class MockAuthRepository : AuthRepository {

    override suspend fun sendMagicLink(email: String): MagicLinkResult {
        delay(1500)
        return MagicLinkResult(success = true)
    }

    override suspend fun verifyMagicLink(token: String): AuthResult {
        delay(800)
        return AuthResult(
            userId = "mock-user-id",
            tokens = AuthTokens(
                accessToken = "mock-access-token",
                refreshToken = "mock-refresh-token",
                expiresIn = 900L
            )
        )
    }

    override suspend fun socialLogin(provider: SocialProvider): AuthResult {
        delay(1200)
        return AuthResult(
            userId = "mock-social-user",
            tokens = AuthTokens(
                accessToken = "mock-social-access-token",
                refreshToken = "mock-social-refresh-token",
                expiresIn = 900L
            )
        )
    }

    override suspend fun refresh(refreshToken: String): AuthResult {
        delay(500)
        return AuthResult(
            userId = "mock-user-id",
            tokens = AuthTokens(
                accessToken = "mock-refreshed-access-token",
                refreshToken = "mock-new-refresh-token",
                expiresIn = 900L
            )
        )
    }

    override suspend fun register(name: String, email: String): Boolean {
        delay(1500)
        return true
    }
}
