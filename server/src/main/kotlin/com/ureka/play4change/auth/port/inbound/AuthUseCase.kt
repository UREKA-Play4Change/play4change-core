package com.ureka.play4change.auth.port.inbound

import com.ureka.play4change.auth.domain.model.TokenPair

interface AuthUseCase {
    fun requestMagicLink(email: String)
    fun verifyMagicLink(token: String): TokenPair
}
