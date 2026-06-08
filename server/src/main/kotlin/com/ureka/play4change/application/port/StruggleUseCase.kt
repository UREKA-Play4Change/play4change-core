package com.ureka.play4change.application.port

import arrow.core.Either
import com.ureka.play4change.domain.struggle.AdaptiveTask
import com.ureka.play4change.domain.struggle.StruggleSession
import com.ureka.play4change.error.AppError

data class SubmitAdaptiveTaskCommand(
    val userId: String,
    val sessionId: String,
    val taskId: String,
    val selectedOption: Int
)

data class AdaptiveSubmitResult(
    val task: AdaptiveTask,
    val isCorrect: Boolean,
    val pointsAwarded: Int,
    val sessionResolved: Boolean,
    /** Non-null only when depth limit was reached and an explanation session was created. */
    val explanationSessionId: String? = null
)

interface StruggleUseCase {
    fun getSession(userId: String, enrollmentId: String): Either<AppError, StruggleSession>
    fun submitAdaptiveTask(command: SubmitAdaptiveTaskCommand): Either<AppError, AdaptiveSubmitResult>
}
