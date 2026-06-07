package com.ureka.play4change.web.user.dto

import com.ureka.play4change.domain.explanation.ExplanationMessage
import com.ureka.play4change.domain.explanation.ExplanationSession
import java.time.OffsetDateTime

data class ExplanationSessionResponse(
    val sessionId: String,
    val status: String,
    val explanationText: String?,
    val messages: List<ExplanationMessageResponse>
) {
    companion object {
        fun from(session: ExplanationSession) = ExplanationSessionResponse(
            sessionId = session.id,
            status = session.status.name,
            explanationText = session.explanationText,
            messages = session.messages.map { ExplanationMessageResponse.from(it) }
        )
    }
}

data class ExplanationMessageResponse(
    val id: String,
    val role: String,
    val content: String,
    val sentAt: OffsetDateTime
) {
    companion object {
        fun from(msg: ExplanationMessage) = ExplanationMessageResponse(
            id = msg.id,
            role = msg.role.name,
            content = msg.content,
            sentAt = msg.sentAt
        )
    }
}

data class SendExplanationMessageRequest(val content: String)
