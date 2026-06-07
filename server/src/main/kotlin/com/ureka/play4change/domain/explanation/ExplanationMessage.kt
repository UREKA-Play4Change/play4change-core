package com.ureka.play4change.domain.explanation

import java.time.OffsetDateTime

data class ExplanationMessage(
    val id: String,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val sentAt: OffsetDateTime
)

enum class MessageRole { USER, AI }
