package com.ureka.play4change.features.splash.domain.repository

import com.ureka.play4change.features.splash.domain.model.SplashData

interface SplashRepository {
    suspend fun checkSession(): SplashData
}
