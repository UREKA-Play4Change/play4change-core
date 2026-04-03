package com.ureka.play4change.domain.enrollment

import com.ureka.play4change.domain.topic.TaskType
import java.time.OffsetDateTime

data class TaskAssignment(
    val id: String,
    val enrollmentId: String,
    val userId: String,
    val taskTemplateId: String,
    val taskTemplateVersion: Int,
    val taskType: TaskType,
    val assignedAt: OffsetDateTime,
    val dueAt: OffsetDateTime,
    val submittedAt: OffsetDateTime?,
    val status: AssignmentStatus,
    val selectedOption: Int?,
    val isCorrect: Boolean?,
    val pointsAwarded: Int,
    val optionOrder: List<Int>,
    val wrongAttemptCount: Int,
    val photoUrl: String?
) {
    fun incrementWrongAttempts(): TaskAssignment =
        copy(wrongAttemptCount = wrongAttemptCount + 1)

    fun markPendingReview(photoUrl: String): TaskAssignment =
        copy(
            submittedAt = OffsetDateTime.now(),
            status = AssignmentStatus.PENDING_REVIEW,
            photoUrl = photoUrl
        )

    fun markSubmitted(
        isCorrect: Boolean,
        pointsAwarded: Int,
        selectedOption: Int? = null,
        photoUrl: String? = null
    ): TaskAssignment {
        val now = OffsetDateTime.now()
        val isLate = now.isAfter(dueAt)
        return copy(
            submittedAt = now,
            status = if (isLate) AssignmentStatus.LATE else AssignmentStatus.SUBMITTED,
            isCorrect = isCorrect,
            pointsAwarded = pointsAwarded,
            selectedOption = selectedOption,
            photoUrl = photoUrl ?: this.photoUrl
        )
    }
}
