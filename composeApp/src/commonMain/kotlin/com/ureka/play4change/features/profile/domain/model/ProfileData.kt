package com.ureka.play4change.features.profile.domain.model

import com.ureka.play4change.core.model.Badge

data class ProfileData(
    val userId: String,
    val name: String,
    val email: String,
    val streakDays: Int,
    val totalPoints: Int,
    val accuracy: Float,
    val level: Int,
    val currentDay: Int,
    val totalDays: Int,
    val badges: List<Badge> = emptyList()
)
