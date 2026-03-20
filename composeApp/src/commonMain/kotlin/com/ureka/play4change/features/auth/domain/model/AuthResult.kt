package com.ureka.play4change.features.auth.domain.model

data class AuthResult(
    val userId: String,
    val token: String
)

enum class SocialProvider { GOOGLE, FACEBOOK }
