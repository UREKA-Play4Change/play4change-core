package com.ureka.play4change.features.auth.data.mock

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

    override suspend fun socialLogin(provider: SocialProvider): AuthResult? {
        delay(1200)
        // TODO: Replace with real OAuth — Google Sign-In SDK / Facebook SDK
        return AuthResult(userId = "mock-social-user", token = "mock-social-token")
    }

    override suspend fun register(name: String, email: String): Boolean {
        delay(1500)
        // TODO: Replace with real registration API call
        return true
    }
}
