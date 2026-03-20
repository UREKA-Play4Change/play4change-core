package com.ureka.play4change.features.profile.data.mock

import com.ureka.play4change.core.model.Badge
import com.ureka.play4change.core.model.BadgeIconType
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
            totalDays = 9,
            badges = listOf(
                Badge("first_task",   "badge_first_task",   "badge_first_task_desc",   BadgeIconType.FIRST_STEP, isUnlocked = true),
                Badge("streak_3",     "badge_streak_3",     "badge_streak_3_desc",     BadgeIconType.FLAME,      isUnlocked = true),
                Badge("streak_7",     "badge_streak_7",     "badge_streak_7_desc",     BadgeIconType.CALENDAR,   isUnlocked = false),
                Badge("perfect_quiz", "badge_perfect_quiz", "badge_perfect_quiz_desc", BadgeIconType.STAR,       isUnlocked = false),
                Badge("first_photo",  "badge_first_photo",  "badge_first_photo_desc",  BadgeIconType.CAMERA,     isUnlocked = false),
                Badge("explorer",     "badge_explorer",     "badge_explorer_desc",     BadgeIconType.COMPASS,    isUnlocked = false),
            )
        )
    }

    override suspend fun signOut() {
        delay(300)
    }
}
