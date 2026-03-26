package com.ureka.play4change.auth.adapter.inbound.web

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)

data class MessageResponse(val message: String)
