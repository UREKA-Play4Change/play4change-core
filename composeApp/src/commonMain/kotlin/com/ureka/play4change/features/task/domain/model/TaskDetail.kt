package com.ureka.play4change.features.task.domain.model

data class TaskDetail(
    val userTaskId: String,
    val title: String,
    val description: String,
    val hint: String,
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val pointsReward: Int,
    val domain: String,
    val content: TaskContent? = null
)

data class SubmitResult(
    val isCorrect: Boolean,
    val pointsAwarded: Int
)
