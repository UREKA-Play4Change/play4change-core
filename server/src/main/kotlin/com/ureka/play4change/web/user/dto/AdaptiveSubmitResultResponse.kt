package com.ureka.play4change.web.user.dto

import com.ureka.play4change.application.port.AdaptiveSubmitResult

data class AdaptiveSubmitResultResponse(
    val isCorrect: Boolean,
    val pointsAwarded: Int,
    val sessionResolved: Boolean
) {
    companion object {
        fun from(r: AdaptiveSubmitResult) = AdaptiveSubmitResultResponse(
            isCorrect = r.isCorrect,
            pointsAwarded = r.pointsAwarded,
            sessionResolved = r.sessionResolved
        )
    }
}
