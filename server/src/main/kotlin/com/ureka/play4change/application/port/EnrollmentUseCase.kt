package com.ureka.play4change.application.port

import arrow.core.Either
import com.ureka.play4change.domain.enrollment.Enrollment
import com.ureka.play4change.error.AppError

data class EnrollCommand(
    val userId: String,
    val topicId: String
)

interface EnrollmentUseCase {
    fun enroll(command: EnrollCommand): Either<AppError, Enrollment>
    fun getEnrollment(userId: String, topicId: String): Either<AppError, Enrollment>
}
