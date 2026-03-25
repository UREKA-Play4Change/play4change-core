package com.ureka.play4change.auth

import kotlinx.serialization.Serializable

@Serializable
enum class AuthProvider { MAGIC_LINK, GOOGLE, FACEBOOK }
