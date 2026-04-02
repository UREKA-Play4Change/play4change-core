package com.ureka.play4change.application.enrollment

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ureka.play4change.application.port.SubmitAnswerCommand
import com.ureka.play4change.application.port.SubmitResult
import com.ureka.play4change.application.port.TaskUseCase
import com.ureka.play4change.application.struggle.ErrorPatternClassifier
import com.ureka.play4change.application.struggle.HandleStruggleService
import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.error.client.Conflict
import com.ureka.play4change.error.client.Forbidden.ResourceOwnershipViolation
import com.ureka.play4change.error.client.NotFound
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class TaskService(
    private val topicRepository: TopicRepository,
    private val topicModuleRepository: TopicModuleRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val handleStruggleService: HandleStruggleService
) : TaskUseCase {

    private val log = LoggerFactory.getLogger(TaskService::class.java)

    override fun getTodayTask(userId: String, topicId: String, timezone: String?): Either<AppError, TaskAssignment> =
        either {
            val enrollment = ensureNotNull(enrollmentRepository.findByUserIdAndTopicId(userId, topicId)) {
                NotFound.ResourceNotFound("Enrollment", "$userId/$topicId")
            }

            val dayIndex = DayIndexCalculator.compute(enrollment.enrolledAt, timezone)

            // Existing assignment for today?
            val assignments = enrollmentRepository.findAssignmentsByEnrollmentId(enrollment.id)
            val existing = assignments.firstOrNull { a ->
                val template = taskTemplateRepository.findById(a.taskTemplateId)
                template?.dayIndex == dayIndex
            }
            if (existing != null) return@either existing

            // Create new assignment for today
            val modules = topicModuleRepository.findByTopicId(topicId)
            val module = ensureNotNull(modules.firstOrNull()) {
                NotFound.ResourceNotFound("TopicModule", topicId)
            }
            val templates = taskTemplateRepository.findCurrentByModuleId(module.id)
            val template = ensureNotNull(templates.firstOrNull { it.dayIndex == dayIndex }) {
                NotFound.ResourceNotFound("TaskTemplate", "dayIndex=$dayIndex")
            }

            val shuffledOrder = (template.options?.indices?.toMutableList() ?: mutableListOf())
                .also { it.shuffle() }
            val now = OffsetDateTime.now()

            enrollmentRepository.saveAssignment(
                TaskAssignment(
                    id = UUID.randomUUID().toString(),
                    enrollmentId = enrollment.id,
                    userId = userId,
                    taskTemplateId = template.id,
                    taskTemplateVersion = template.version,
                    taskType = template.taskType,
                    assignedAt = now,
                    dueAt = now.plusHours(24),
                    submittedAt = null,
                    status = AssignmentStatus.PENDING,
                    selectedOption = null,
                    isCorrect = null,
                    pointsAwarded = 0,
                    optionOrder = shuffledOrder,
                    wrongAttemptCount = 0,
                    photoUrl = null
                )
            )
        }

    override fun submitAnswer(command: SubmitAnswerCommand): Either<AppError, SubmitResult> = either {
        val assignment = ensureNotNull(enrollmentRepository.findAssignmentById(command.assignmentId)) {
            NotFound.ResourceNotFound("TaskAssignment", command.assignmentId)
        }
        ensure(assignment.userId == command.userId) {
            ResourceOwnershipViolation("TaskAssignment")
        }
        ensure(assignment.status == AssignmentStatus.PENDING) {
            Conflict.ConcurrentModification
        }
        ensure(command.selectedOption >= 0) {
            BadRequest.InvalidField("selectedOption", "must be >= 0")
        }

        val template = ensureNotNull(taskTemplateRepository.findById(assignment.taskTemplateId)) {
            NotFound.ResourceNotFound("TaskTemplate", assignment.taskTemplateId)
        }

        val originalIndex = assignment.optionOrder.getOrNull(command.selectedOption)
        ensure(originalIndex != null) {
            BadRequest.InvalidField("selectedOption", "out of range")
        }

        val isCorrect = originalIndex == template.correctAnswer
        val now = OffsetDateTime.now()
        val isLate = now.isAfter(assignment.dueAt)

        val pointsAwarded = when {
            !isCorrect -> 0
            isLate -> template.pointsReward / 2
            else -> template.pointsReward
        }

        var struggleTriggered = false

        val updatedAssignment = if (isCorrect || assignment.wrongAttemptCount >= 1) {
            // Final submission (correct OR 2nd wrong attempt)
            if (!isCorrect && assignment.wrongAttemptCount == 1) {
                // 2nd wrong — classify and trigger struggle
                val pattern = ErrorPatternClassifier.classify(assignment, command.selectedOption, template)
                handleStruggleService.triggerAsync(
                    enrollmentId = assignment.enrollmentId,
                    assignmentId = assignment.id,
                    errorPattern = pattern,
                    template = template,
                    userId = command.userId
                )
                struggleTriggered = true
                log.info(
                    "Struggle triggered for user {} on assignment {}, pattern={}",
                    command.userId, assignment.id, pattern
                )
            }
            assignment.markSubmitted(
                isCorrect = isCorrect,
                pointsAwarded = pointsAwarded,
                selectedOption = command.selectedOption
            )
        } else {
            // 1st wrong — record attempt, keep PENDING
            assignment.copy(
                wrongAttemptCount = assignment.wrongAttemptCount + 1,
                selectedOption = command.selectedOption
            )
        }

        val savedAssignment = enrollmentRepository.saveAssignment(updatedAssignment)

        // Update enrollment points + streak only on final correct submission
        val enrollment = ensureNotNull(enrollmentRepository.findById(assignment.enrollmentId)) {
            NotFound.ResourceNotFound("Enrollment", assignment.enrollmentId)
        }

        val updatedEnrollment = if (isCorrect) {
            enrollment.addPoints(pointsAwarded).incrementStreak()
        } else if (updatedAssignment.status != AssignmentStatus.PENDING) {
            enrollment.resetStreak()
        } else {
            enrollment
        }

        val savedEnrollment = enrollmentRepository.save(updatedEnrollment)

        SubmitResult(
            assignment = savedAssignment,
            isCorrect = isCorrect,
            pointsAwarded = pointsAwarded,
            totalPoints = savedEnrollment.totalPointsEarned,
            streakDays = savedEnrollment.streakDays,
            struggleTriggered = struggleTriggered
        )
    }
}
