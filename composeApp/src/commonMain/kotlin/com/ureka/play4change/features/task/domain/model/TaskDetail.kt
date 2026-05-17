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
    val content: TaskContent? = null,
    /** ISO-8601 deadline returned by the server (e.g. "2026-05-08T12:00:00Z"). */
    val dueAt: String = "",
    /** Number of wrong submissions already made for this assignment. */
    val wrongAttemptCount: Int = 0
)

data class SubmitResult(
    val isCorrect: Boolean,
    val pointsAwarded: Int,
    val totalPoints: Int = 0,
    val streakDays: Int = 0,
    val struggleTriggered: Boolean = false
)
