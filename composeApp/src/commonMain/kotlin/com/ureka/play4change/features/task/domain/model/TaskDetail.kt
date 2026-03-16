package com.ureka.play4change.features.task.domain.model

data class TaskDetail(
    val userTaskId: String,
    val title: String,
    val description: String,
    val hint: String,
    val options: List<String>,
    val correctIndex: Int,
    val pointsReward: Int,
    val domain: String
)

data class SubmitResult(
    val isCorrect: Boolean,
    val pointsAwarded: Int
)
