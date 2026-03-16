package com.ureka.play4change.features.auth.data.mock

import com.ureka.play4change.features.auth.domain.model.MagicLinkResult
import com.ureka.play4change.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.delay

class MockAuthRepository : AuthRepository {
    override suspend fun sendMagicLink(email: String): MagicLinkResult {
        delay(1500)
        return MagicLinkResult(success = true)
    }
}
