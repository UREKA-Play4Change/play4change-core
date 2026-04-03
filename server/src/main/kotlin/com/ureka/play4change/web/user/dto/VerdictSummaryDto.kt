package com.ureka.play4change.web.user.dto

import com.ureka.play4change.application.port.VerdictSummary

data class VerdictSummaryDto(
    val correct: Int,
    val incorrect: Int,
    val total: Int
) {
    companion object {
        fun from(s: VerdictSummary) = VerdictSummaryDto(
            correct = s.correct,
            incorrect = s.incorrect,
            total = s.total
        )
    }
}
