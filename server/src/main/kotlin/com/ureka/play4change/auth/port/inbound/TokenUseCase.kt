package com.ureka.play4change.auth.port.inbound

import com.ureka.play4change.auth.domain.model.TokenPair

interface TokenUseCase {
    fun refresh(rawRefreshToken: String): TokenPair
    fun revoke(rawRefreshToken: String)
}
