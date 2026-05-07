package com.ureka.play4change.application.user

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.ureka.play4change.application.port.EnrollmentUseCase
import com.ureka.play4change.application.port.TopicUseCase
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.NotFound
import org.springframework.stereotype.Service

@Service
class GetUserProfileService(
    private val userRepository: UserRepository,
    private val enrollmentUseCase: EnrollmentUseCase,
    private val enrollmentRepository: EnrollmentRepository,
    private val topicUseCase: TopicUseCase
) : GetUserProfileUseCase {

    override fun execute(userId: String): Either<AppError, UserProfile> {
        val user = userRepository.findById(userId)
            ?: return NotFound.ResourceNotFound("User", userId).left()

        val enrollment = enrollmentUseCase.getActiveEnrollments(userId).firstOrNull()

        val totalPoints = enrollment?.totalPointsEarned ?: 0
        val streakDays = enrollment?.streakDays ?: 0
        val currentDay = if (enrollment != null) enrollment.currentDayIndex + 1 else 0
        val totalDays = enrollment?.let {
            topicUseCase.getById(it.topicId).fold(ifLeft = { 0 }, ifRight = { t -> t.taskCount })
        } ?: 0

        val accuracy = enrollment?.let { e ->
            val assignments = enrollmentRepository.findAssignmentsByEnrollmentId(e.id)
            val submitted = assignments.filter { it.isCorrect != null }
            if (submitted.isEmpty()) 0.0f
            else submitted.count { it.isCorrect == true }.toFloat() / submitted.size.toFloat()
        } ?: 0.0f

        val level = totalPoints / 100 + 1

        return UserProfile(
            userId = user.id,
            name = user.name ?: user.email,
            email = user.email,
            streakDays = streakDays,
            totalPoints = totalPoints,
            accuracy = accuracy,
            level = level,
            currentDay = currentDay,
            totalDays = totalDays
        ).right()
    }
}
