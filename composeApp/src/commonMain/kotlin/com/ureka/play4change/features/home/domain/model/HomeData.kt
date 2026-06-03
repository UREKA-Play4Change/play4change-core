package com.ureka.play4change.features.home.domain.model

import com.ureka.play4change.core.model.RoadmapNode
import com.ureka.play4change.design.components.DayStatus

data class HomeData(
    val userName: String,
    val streakDays: Int,
    val totalPoints: Int,
    val level: Int,
    val xpProgress: Float,
    val weekProgress: List<DayStatus>,
    val roadmapNodes: List<RoadmapNode>,
    val todayTasks: List<TaskSummaryWithTopic>,
    val pendingReviews: List<PendingReviewSummary> = emptyList(),
    val isEnrolled: Boolean = true
)

/** A peer-review assignment waiting for the learner's verdict. */
data class PendingReviewSummary(
    val reviewId: String,
    val topicTitle: String,
    val photoUrl: String?
)

/** Per-topic daily task entry shown on the home screen. */
data class TaskSummaryWithTopic(
    val topicId: String,
    val topicTitle: String,
    /** Null when the task is still generating or unavailable. */
    val task: TaskSummary?,
    /** True when the learner already submitted their answer today. */
    val completed: Boolean,
    /** True when the server returned 202 — AI generation is in progress. */
    val isGenerating: Boolean = false,
    /** True when the server returned 409 — an adaptive struggle session is open. */
    val struggleOpen: Boolean = false,
    /** The enrollment ID associated with the open struggle session. */
    val struggleEnrollmentId: String = "",
    /** True when the server returned 404 with X-Task-Available-At — next task is coming soon. */
    val isWaitingForNext: Boolean = false
)

data class TaskSummary(
    val id: String,
    val title: String,
    val domain: String,
    val pointsReward: Int
)
