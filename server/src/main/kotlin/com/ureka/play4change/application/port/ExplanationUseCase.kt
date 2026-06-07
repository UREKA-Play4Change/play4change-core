package com.ureka.play4change.application.port

import arrow.core.Either
import com.ureka.play4change.domain.explanation.ExplanationMessage
import com.ureka.play4change.domain.explanation.ExplanationSession
import com.ureka.play4change.error.AppError

data class SendExplanationMessageCommand(
    val userId: String,
    val sessionId: String,
    val content: String
)

data class ResolveExplanationCommand(
    val userId: String,
    val sessionId: String
)

interface ExplanationUseCase {
    fun getSession(userId: String, sessionId: String): Either<AppError, ExplanationSession>
    fun sendMessage(command: SendExplanationMessageCommand): Either<AppError, ExplanationMessage>
    fun resolve(command: ResolveExplanationCommand): Either<AppError, Unit>
    fun triggerAsync(enrollmentId: String, originalTaskAssignmentId: String, errorPattern: String, userId: String)
}
