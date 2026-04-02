package com.ureka.play4change.domain.struggle

import java.time.OffsetDateTime

data class AdaptiveTask(
    val id: String,
    val struggleSessionId: String,
    val title: String,
    val description: String,
    val hint: String?,
    val pointsReward: Int,
    val orderIndex: Int,
    val completedAt: OffsetDateTime?,
    val isCorrect: Boolean?,
    val options: List<String>?,
    val correctAnswer: Int?,
    val selectedOption: Int?,
    val optionOrder: List<Int>
)
