package com.ureka.play4change.application.port

import arrow.core.Either
import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.domain.peerreview.PeerReview
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.error.AppError
import java.time.OffsetDateTime

sealed class TodayTaskResult {
    data class Available(val assignment: TaskAssignment, val template: TaskTemplate) : TodayTaskResult()
    data class GenerationPending(val language: String) : TodayTaskResult()
    data class NotAvailableYet(val availableAt: OffsetDateTime) : TodayTaskResult()
    data class StruggleOpen(val enrollmentId: String) : TodayTaskResult()
}

data class SubmitPhotoCommand(
    val userId: String,
    val assignmentId: String,
    val photoUrl: String
)

data class SubmitTodoResult(
    val assignment: TaskAssignment,
    val assignedReview: PeerReview?,
    val assignedReviewPhotoUrl: String?
)

data class SubmitAnswerCommand(
    val userId: String,
    val assignmentId: String,
    val selectedOption: Int,
    val timezone: String?
)

data class SubmitResult(
    val assignment: TaskAssignment,
    val isCorrect: Boolean,
    val pointsAwarded: Int,
    val totalPoints: Int,
    val streakDays: Int,
    val struggleTriggered: Boolean
)

interface TaskUseCase {
    fun getTodayTask(userId: String, topicId: String, timezone: String?): Either<AppError, TodayTaskResult>
    fun submitAnswer(command: SubmitAnswerCommand): Either<AppError, SubmitResult>
    fun submitPhoto(command: SubmitPhotoCommand): Either<AppError, SubmitTodoResult>
}
