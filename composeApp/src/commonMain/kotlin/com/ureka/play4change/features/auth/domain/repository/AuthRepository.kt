package com.ureka.play4change.features.auth.domain.repository

import com.ureka.play4change.features.auth.domain.model.AuthResult
import com.ureka.play4change.features.auth.domain.model.MagicLinkResult
import com.ureka.play4change.features.auth.domain.model.SocialProvider

interface AuthRepository {
    suspend fun sendMagicLink(email: String): MagicLinkResult
    suspend fun socialLogin(provider: SocialProvider): AuthResult?
    suspend fun register(name: String, email: String): Boolean
}
