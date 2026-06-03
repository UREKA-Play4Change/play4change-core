package com.ureka.play4change.application.user

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.ureka.play4change.application.port.EnrollmentUseCase
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.NotFound
import org.springframework.stereotype.Service

@Service
class GetUserProfileService(
    private val userRepository: UserRepository,
    private val enrollmentUseCase: EnrollmentUseCase,
    private val enrollmentRepository: EnrollmentRepository
) : GetUserProfileUseCase {

    override fun execute(userId: String): Either<AppError, UserProfile> {
        val user = userRepository.findById(userId)
            ?: return NotFound.ResourceNotFound("User", userId).left()

        val allEnrollments = enrollmentUseCase.getActiveEnrollments(userId) +
            enrollmentRepository.findCompletedByUserId(userId)

        val totalPoints = allEnrollments.sumOf { it.totalPointsEarned }
        val streakDays = allEnrollments.maxOfOrNull { it.streakDays } ?: 0

        val allAssignments = allEnrollments.flatMap { e ->
            enrollmentRepository.findAssignmentsByEnrollmentId(e.id)
        }
        val submitted = allAssignments.filter { it.isCorrect != null }
        val accuracy = if (submitted.isEmpty()) 0.0f
            else submitted.count { it.isCorrect == true }.toFloat() / submitted.size.toFloat()

        return UserProfile(
            userId = user.id,
            name = user.name ?: user.email,
            email = user.email,
            streakDays = streakDays,
            totalPoints = totalPoints,
            accuracy = accuracy,
            preferredLanguage = user.preferredLanguage
        ).right()
    }
}
