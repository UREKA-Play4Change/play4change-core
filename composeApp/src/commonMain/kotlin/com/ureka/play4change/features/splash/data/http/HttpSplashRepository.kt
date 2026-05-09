package com.ureka.play4change.features.splash.data.http

import com.ureka.play4change.core.network.TokenStorage
import com.ureka.play4change.features.splash.domain.model.SplashData
import com.ureka.play4change.features.splash.domain.repository.SplashRepository

/**
 * Checks whether a valid access token is persisted in [TokenStorage].
 * If a token exists the session is considered active and the user is
 * routed to Home; otherwise they are routed to Login.
 */
class HttpSplashRepository(
    private val tokenStorage: TokenStorage
) : SplashRepository {

    override suspend fun checkSession(): SplashData =
        SplashData(isAuthenticated = tokenStorage.getAccessToken() != null)
}
