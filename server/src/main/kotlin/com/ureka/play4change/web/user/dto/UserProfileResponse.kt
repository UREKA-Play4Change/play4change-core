package com.ureka.play4change.web.user.dto

import com.ureka.play4change.application.user.UserProfile

data class UserProfileResponse(
    val userId: String,
    val name: String,
    val email: String,
    val streakDays: Int,
    val totalPoints: Int,
    val accuracy: Float,
    val level: Int,
    val currentDay: Int,
    val totalDays: Int
) {
    companion object {
        fun from(profile: UserProfile) = UserProfileResponse(
            userId = profile.userId,
            name = profile.name,
            email = profile.email,
            streakDays = profile.streakDays,
            totalPoints = profile.totalPoints,
            accuracy = profile.accuracy,
            level = profile.level,
            currentDay = profile.currentDay,
            totalDays = profile.totalDays
        )
    }
}
