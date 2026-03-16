package com.ureka.play4change.features.splash.data.mock

import com.ureka.play4change.features.splash.domain.model.SplashData
import com.ureka.play4change.features.splash.domain.repository.SplashRepository
import kotlinx.coroutines.delay

class MockSplashRepository : SplashRepository {
    override suspend fun checkSession(): SplashData {
        delay(1500)
        return SplashData(isAuthenticated = false)
    }
}
