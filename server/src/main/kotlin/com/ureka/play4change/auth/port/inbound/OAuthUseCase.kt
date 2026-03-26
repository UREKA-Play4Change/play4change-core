package com.ureka.play4change.auth.port.inbound

import com.ureka.play4change.auth.domain.model.AuthProvider
import com.ureka.play4change.auth.domain.model.TokenPair

interface OAuthUseCase {
    fun loginOrRegister(provider: AuthProvider, idToken: String): TokenPair
}
