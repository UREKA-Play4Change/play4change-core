package com.ureka.play4change.auth.domain.model

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresInSeconds: Long
)
