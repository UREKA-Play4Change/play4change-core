package com.ureka.play4change.features.profile.domain.repository

import com.ureka.play4change.features.profile.domain.model.ProfileData

interface ProfileRepository {
    suspend fun getProfile(userId: String): ProfileData
    suspend fun updateName(name: String): ProfileData
    suspend fun updatePreferences(language: String)
    suspend fun signOut()
}
