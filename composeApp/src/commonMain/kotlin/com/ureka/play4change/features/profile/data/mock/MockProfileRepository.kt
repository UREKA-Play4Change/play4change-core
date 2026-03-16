package com.ureka.play4change.features.profile.data.mock

import com.ureka.play4change.features.profile.domain.model.ProfileData
import com.ureka.play4change.features.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.delay

class MockProfileRepository : ProfileRepository {
    override suspend fun getProfile(userId: String): ProfileData {
        delay(500)
        return ProfileData(
            userId = "current-user",
            name = "Radesh Govind",
            email = "radesh.govind@gmail.com",
            streakDays = 7,
            totalPoints = 1340,
            accuracy = 0.83f,
            level = 4,
            currentDay = 6,
            totalDays = 9
        )
    }

    override suspend fun signOut() {
        delay(300)
    }
}
