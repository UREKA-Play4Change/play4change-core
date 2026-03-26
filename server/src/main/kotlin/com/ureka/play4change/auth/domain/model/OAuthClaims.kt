package com.ureka.play4change.auth.domain.model

data class OAuthClaims(
    val email: String,
    val name: String?,
    val sub: String
)
