package com.ureka.play4change.application.user

data class UserProfile(
    val userId: String,
    val name: String,
    val email: String,
    val streakDays: Int,
    val totalPoints: Int,
    val accuracy: Float,
    val preferredLanguage: String
)
