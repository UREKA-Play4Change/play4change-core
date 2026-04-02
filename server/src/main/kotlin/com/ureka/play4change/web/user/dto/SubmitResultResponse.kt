package com.ureka.play4change.web.user.dto

import com.ureka.play4change.application.port.SubmitResult

data class SubmitResultResponse(
    val isCorrect: Boolean,
    val pointsAwarded: Int,
    val totalPoints: Int,
    val streakDays: Int,
    val struggleTriggered: Boolean
) {
    companion object {
        fun from(r: SubmitResult) = SubmitResultResponse(
            isCorrect = r.isCorrect,
            pointsAwarded = r.pointsAwarded,
            totalPoints = r.totalPoints,
            streakDays = r.streakDays,
            struggleTriggered = r.struggleTriggered
        )
    }
}
