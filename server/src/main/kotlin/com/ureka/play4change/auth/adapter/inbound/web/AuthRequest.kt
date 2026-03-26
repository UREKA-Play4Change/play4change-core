package com.ureka.play4change.auth.adapter.inbound.web

import com.ureka.play4change.auth.domain.model.AuthProvider

data class MagicLinkRequest(val email: String)
data class OAuthRequest(val provider: AuthProvider, val idToken: String)
data class RefreshRequest(val refreshToken: String)
