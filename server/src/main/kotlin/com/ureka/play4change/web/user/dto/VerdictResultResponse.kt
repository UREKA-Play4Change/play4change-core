package com.ureka.play4change.web.user.dto

import com.ureka.play4change.application.port.VerdictResult

data class VerdictResultResponse(
    val verdict: String,
    val currentVerdicts: VerdictSummaryDto,
    val finalized: Boolean,
    val submitterPointsAwarded: Int?
) {
    companion object {
        fun from(r: VerdictResult) = VerdictResultResponse(
            verdict = r.peerReview.verdict!!.name,
            currentVerdicts = VerdictSummaryDto.from(r.verdictSummary),
            finalized = r.finalized,
            submitterPointsAwarded = r.submitterPointsAwarded
        )
    }
}
