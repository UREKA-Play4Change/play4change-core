package com.ureka.play4change.auth.port.outbound

import com.ureka.play4change.auth.domain.model.AuthProvider
import com.ureka.play4change.auth.domain.model.OAuthClaims

interface OAuthVerifierPort {
    fun verify(provider: AuthProvider, idToken: String): OAuthClaims
}
