package com.ureka.play4change.web.user.dto

import com.ureka.play4change.application.port.AdaptiveSubmitResult

data class AdaptiveSubmitResultResponse(
    val taskId: String,
    val title: String,
    val isCorrect: Boolean,
    val pointsAwarded: Int,
    val sessionResolved: Boolean,
    val explanationSessionId: String?
) {
    companion object {
        fun from(r: AdaptiveSubmitResult) = AdaptiveSubmitResultResponse(
            taskId = r.task.id,
            title = r.task.title,
            isCorrect = r.isCorrect,
            pointsAwarded = r.pointsAwarded,
            sessionResolved = r.sessionResolved,
            explanationSessionId = r.explanationSessionId
        )
    }
}
