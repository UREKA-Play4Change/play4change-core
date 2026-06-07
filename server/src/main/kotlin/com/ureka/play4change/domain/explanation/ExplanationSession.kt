package com.ureka.play4change.domain.explanation

import java.time.OffsetDateTime

data class ExplanationSession(
    val id: String,
    val enrollmentId: String,
    val originalTaskAssignmentId: String,
    val errorPattern: String,
    val explanationText: String?,
    val status: ExplanationStatus,
    val generatedAt: OffsetDateTime,
    val resolvedAt: OffsetDateTime?,
    val messages: List<ExplanationMessage>
) {
    fun activate(text: String): ExplanationSession =
        copy(status = ExplanationStatus.ACTIVE, explanationText = text)

    fun resolve(): ExplanationSession =
        copy(status = ExplanationStatus.RESOLVED, resolvedAt = OffsetDateTime.now())
}

enum class ExplanationStatus { GENERATING, ACTIVE, RESOLVED }
