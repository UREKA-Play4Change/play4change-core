package com.ureka.play4change.web.user.dto

import com.ureka.play4change.domain.struggle.StruggleSession

data class StruggleSessionResponse(
    val sessionId: String,
    val errorPattern: String,
    val status: String,
    val adaptiveTasks: List<AdaptiveTaskResponse>
) {
    companion object {
        fun from(session: StruggleSession) = StruggleSessionResponse(
            sessionId = session.id,
            errorPattern = session.errorPattern.name,
            status = session.status.name,
            adaptiveTasks = session.adaptiveTasks
                .sortedBy { it.orderIndex }
                .map { AdaptiveTaskResponse.from(it) }
        )
    }
}
