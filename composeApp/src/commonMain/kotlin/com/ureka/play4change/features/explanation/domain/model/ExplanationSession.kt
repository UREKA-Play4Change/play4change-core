package com.ureka.play4change.features.explanation.domain.model

data class ExplanationSession(
    val sessionId: String,
    val status: String,       // GENERATING | ACTIVE | RESOLVED
    val explanationText: String?,
    val messages: List<ExplanationMessage>
)

data class ExplanationMessage(
    val id: String,
    val role: String,         // USER | AI
    val content: String
)
