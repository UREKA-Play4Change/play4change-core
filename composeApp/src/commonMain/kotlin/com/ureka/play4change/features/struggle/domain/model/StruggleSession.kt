package com.ureka.play4change.features.struggle.domain.model

data class StruggleSession(
    val sessionId: String,
    val errorPattern: String,
    val status: String,
    val tasks: List<AdaptiveTask>
)

data class AdaptiveTask(
    val taskId: String,
    val title: String,
    val description: String,
    val hint: String,
    val options: List<String>,
    val pointsReward: Int
)
