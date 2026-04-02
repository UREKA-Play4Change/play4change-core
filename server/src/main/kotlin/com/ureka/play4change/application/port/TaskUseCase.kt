package com.ureka.play4change.application.port

import arrow.core.Either
import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.error.AppError

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
    fun getTodayTask(userId: String, topicId: String, timezone: String?): Either<AppError, TaskAssignment>
    fun submitAnswer(command: SubmitAnswerCommand): Either<AppError, SubmitResult>
}
