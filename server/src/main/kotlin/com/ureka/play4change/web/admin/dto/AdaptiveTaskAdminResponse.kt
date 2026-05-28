package com.ureka.play4change.web.admin.dto

import com.ureka.play4change.domain.struggle.AdaptiveTaskAdminView
import java.time.OffsetDateTime

data class AdaptiveTaskAdminResponse(
    val id: String,
    val sessionId: String,
    val sessionStatus: String,
    val errorPattern: String,
    val sessionDetectedAt: OffsetDateTime,
    val enrollmentId: String,
    val title: String,
    val description: String,
    val hint: String?,
    val pointsReward: Int,
    val orderIndex: Int,
    val options: List<String>?,
    val correctAnswer: Int?,
    val isCorrect: Boolean?,
    val completedAt: OffsetDateTime?
) {
    companion object {
        fun from(view: AdaptiveTaskAdminView) = AdaptiveTaskAdminResponse(
            id = view.task.id,
            sessionId = view.sessionId,
            sessionStatus = view.sessionStatus.name,
            errorPattern = view.errorPattern.name,
            sessionDetectedAt = view.sessionDetectedAt,
            enrollmentId = view.enrollmentId,
            title = view.task.title,
            description = view.task.description,
            hint = view.task.hint,
            pointsReward = view.task.pointsReward,
            orderIndex = view.task.orderIndex,
            options = view.task.options,
            correctAnswer = view.task.correctAnswer,
            isCorrect = view.task.isCorrect,
            completedAt = view.task.completedAt
        )
    }
}
