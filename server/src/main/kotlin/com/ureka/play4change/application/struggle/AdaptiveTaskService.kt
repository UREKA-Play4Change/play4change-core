package com.ureka.play4change.application.struggle

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ureka.play4change.application.port.AdaptiveSubmitResult
import com.ureka.play4change.application.port.StruggleUseCase
import com.ureka.play4change.application.port.SubmitAdaptiveTaskCommand
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.struggle.StruggleRepository
import com.ureka.play4change.domain.struggle.StruggleSession
import com.ureka.play4change.domain.struggle.StruggleStatus
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.error.client.Conflict
import com.ureka.play4change.error.client.Forbidden.ResourceOwnershipViolation
import com.ureka.play4change.error.client.NotFound
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class AdaptiveTaskService(
    private val struggleRepository: StruggleRepository,
    private val enrollmentRepository: EnrollmentRepository
) : StruggleUseCase {

    private val log = LoggerFactory.getLogger(AdaptiveTaskService::class.java)

    override fun getSession(userId: String, enrollmentId: String): Either<AppError, StruggleSession> = either {
        val enrollment = ensureNotNull(enrollmentRepository.findById(enrollmentId)) {
            NotFound.ResourceNotFound("Enrollment", enrollmentId)
        }
        ensure(enrollment.userId == userId) {
            ResourceOwnershipViolation("Enrollment")
        }
        ensureNotNull(struggleRepository.findOpenByEnrollmentId(enrollmentId)) {
            NotFound.ResourceNotFound("StruggleSession", enrollmentId)
        }
    }

    override fun submitAdaptiveTask(command: SubmitAdaptiveTaskCommand): Either<AppError, AdaptiveSubmitResult> =
        either {
            val session = ensureNotNull(struggleRepository.findById(command.sessionId)) {
                NotFound.ResourceNotFound("StruggleSession", command.sessionId)
            }
            ensure(session.status == StruggleStatus.OPEN) {
                Conflict.ConcurrentModification
            }

            val enrollment = ensureNotNull(enrollmentRepository.findById(session.enrollmentId)) {
                NotFound.ResourceNotFound("Enrollment", session.enrollmentId)
            }
            ensure(enrollment.userId == command.userId) {
                ResourceOwnershipViolation("Enrollment")
            }

            val task = ensureNotNull(session.adaptiveTasks.firstOrNull { it.id == command.taskId }) {
                NotFound.ResourceNotFound("AdaptiveTask", command.taskId)
            }
            ensure(task.completedAt == null) {
                Conflict.ConcurrentModification
            }
            ensure(command.selectedOption >= 0) {
                BadRequest.InvalidField("selectedOption", "must be >= 0")
            }

            val originalIndex = task.optionOrder.getOrNull(command.selectedOption)
            ensure(originalIndex != null) {
                BadRequest.InvalidField("selectedOption", "out of range")
            }

            val isCorrect = originalIndex == task.correctAnswer
            val pointsAwarded = if (isCorrect) task.pointsReward else 0

            val updatedTask = task.copy(
                completedAt = OffsetDateTime.now(),
                isCorrect = isCorrect,
                selectedOption = command.selectedOption
            )

            val updatedTasks = session.adaptiveTasks.map {
                if (it.id == command.taskId) updatedTask else it
            }
            val updatedSession = session.copy(adaptiveTasks = updatedTasks)

            val allComplete = updatedTasks.all { it.completedAt != null }
            val resolvedSession = if (allComplete) updatedSession.resolve() else updatedSession

            struggleRepository.save(resolvedSession)

            if (isCorrect) {
                val updatedEnrollment = enrollment.addPoints(pointsAwarded)
                enrollmentRepository.save(updatedEnrollment)
            }

            if (allComplete) {
                log.info(
                    "Struggle session {} resolved for enrollment {}",
                    session.id, session.enrollmentId
                )
            }

            AdaptiveSubmitResult(
                task = updatedTask,
                isCorrect = isCorrect,
                pointsAwarded = pointsAwarded,
                sessionResolved = allComplete
            )
        }
}
