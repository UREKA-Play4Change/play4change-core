package com.ureka.play4change.domain.struggle

import java.time.OffsetDateTime

data class StruggleSession(
    val id: String,
    val enrollmentId: String,
    val originalTaskAssignmentId: String,
    val errorPattern: ErrorPattern,
    val attemptCount: Int,
    val detectedAt: OffsetDateTime,
    val resolvedAt: OffsetDateTime?,
    val status: StruggleStatus,
    val adaptiveTasks: List<AdaptiveTask>
) {
    fun resolve(): StruggleSession =
        copy(status = StruggleStatus.RESOLVED, resolvedAt = OffsetDateTime.now())

    fun abandon(): StruggleSession =
        copy(status = StruggleStatus.ABANDONED, resolvedAt = OffsetDateTime.now())
}
