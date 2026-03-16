package com.ureka.play4change.features.auth.domain.repository

import com.ureka.play4change.features.auth.domain.model.MagicLinkResult

interface AuthRepository {
    suspend fun sendMagicLink(email: String): MagicLinkResult
}
