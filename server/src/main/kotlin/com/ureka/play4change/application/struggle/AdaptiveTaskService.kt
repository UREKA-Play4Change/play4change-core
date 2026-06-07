package com.ureka.play4change.application.struggle

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ureka.play4change.application.port.AdaptiveSubmitResult
import com.ureka.play4change.application.port.StruggleUseCase
import com.ureka.play4change.application.port.SubmitAdaptiveTaskCommand
import com.ureka.play4change.application.explanation.ExplanationService
import com.ureka.play4change.domain.enrollment.AssignmentStatus
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

private const val MAX_STRUGGLE_DEPTH = 3

@Service
class AdaptiveTaskService(
    private val struggleRepository: StruggleRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val handleStruggleService: HandleStruggleService,
    private val explanationService: ExplanationService
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

    @Suppress("LongMethod") // struggle resolution state machine — all branches must stay together for correctness
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
            val pointsAwarded = 0 // adaptive tasks never award score points

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
            val allCorrect = allComplete && updatedTasks.all { it.isCorrect == true }
            val resolvedSession = if (allComplete) updatedSession.resolve() else updatedSession

            struggleRepository.save(resolvedSession)

            val enrollmentToSave = if (allCorrect) enrollment.incrementStreak() else enrollment
            if (enrollmentToSave !== enrollment) enrollmentRepository.save(enrollmentToSave)

            if (allComplete) {
                if (allCorrect) {
                    // All adaptive tasks passed — reset original assignment so the learner can retry the main task
                    val originalAssignment = enrollmentRepository.findAssignmentById(session.originalTaskAssignmentId)
                    if (originalAssignment != null) {
                        enrollmentRepository.saveAssignment(
                            originalAssignment.copy(
                                status = AssignmentStatus.PENDING,
                                submittedAt = null,
                                selectedOption = null,
                                isCorrect = null,
                                pointsAwarded = 0
                                // wrongAttemptCount intentionally preserved — failure history must survive
                                // the reset so stats can account for it when the user retries
                            )
                        )
                    }
                    log.info(
                        "Struggle session {} resolved successfully — original assignment {} reset to PENDING",
                        session.id, session.originalTaskAssignmentId
                    )
                } else {
                    // One or more adaptive tasks failed — spawn a follow-up only if under depth limit
                    // Only count sessions where the learner actually received adaptive tasks.
                    // ABANDONED sessions (AI timed out / generation failed) must not consume
                    // depth slots — the learner never got help from them.
                    val depthForAssignment = struggleRepository
                        .findAllByEnrollmentId(session.enrollmentId)
                        .count {
                            it.originalTaskAssignmentId == session.originalTaskAssignmentId &&
                                it.status != StruggleStatus.ABANDONED
                        }
                    if (depthForAssignment < MAX_STRUGGLE_DEPTH) {
                        handleStruggleService.triggerFromPreviousSession(resolvedSession, command.userId)
                        log.info(
                            "Struggle session {} had failures — follow-up session triggered (depth {}/{})",
                            session.id, depthForAssignment, MAX_STRUGGLE_DEPTH
                        )
                    } else {
                        // Max depth reached — trigger AI explanation mode instead of immediately resetting.
                        // The original assignment stays SUBMITTED; ExplanationService resets it when
                        // the learner clicks "I Understood" (or if AI generation fails as a fallback).
                        explanationService.triggerAsync(
                            enrollmentId = session.enrollmentId,
                            originalTaskAssignmentId = session.originalTaskAssignmentId,
                            errorPattern = session.errorPattern.name,
                            userId = command.userId
                        )
                        log.info(
                            "Struggle session {} reached max depth ({}) — explanation triggered for assignment {}",
                            session.id, MAX_STRUGGLE_DEPTH, session.originalTaskAssignmentId
                        )
                    }
                }
            }

            AdaptiveSubmitResult(
                task = updatedTask,
                isCorrect = isCorrect,
                pointsAwarded = pointsAwarded,
                sessionResolved = allComplete
            )
        }
}
