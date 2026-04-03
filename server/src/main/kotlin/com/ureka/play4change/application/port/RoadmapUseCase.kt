package com.ureka.play4change.application.port

import arrow.core.Either
import com.ureka.play4change.error.AppError

enum class RoadmapNodeStatus {
    COMPLETED, LATE, SKIPPED, PENDING, LOCKED, ADAPTIVE_PENDING, ADAPTIVE_COMPLETED,
    PENDING_REVIEW, REVIEW_PENDING
}

data class RoadmapNode(
    val dayIndex: Int,
    val title: String,
    val status: RoadmapNodeStatus,
    val isAdaptive: Boolean,
    val assignmentId: String?,
    val pointsAwarded: Int?
)

interface RoadmapUseCase {
    fun getRoadmap(userId: String, topicId: String, timezone: String?): Either<AppError, List<RoadmapNode>>
}
