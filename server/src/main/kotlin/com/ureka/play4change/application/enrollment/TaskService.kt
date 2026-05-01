package com.ureka.play4change.application.enrollment

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ureka.play4change.application.port.BadgeIssuancePort
import com.ureka.play4change.application.port.PeerReviewUseCase
import com.ureka.play4change.application.port.SubmitAnswerCommand
import com.ureka.play4change.application.port.SubmitPhotoCommand
import com.ureka.play4change.application.port.SubmitResult
import com.ureka.play4change.application.port.SubmitTodoResult
import com.ureka.play4change.application.port.TaskUseCase
import com.ureka.play4change.application.port.TodayTaskResult
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.application.struggle.ErrorPatternClassifier
import com.ureka.play4change.application.struggle.HandleStruggleService
import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.domain.enrollment.TaskShuffleSeed
import com.ureka.play4change.domain.topic.TaskInstanceRepository
import com.ureka.play4change.domain.topic.TaskType
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.error.client.Conflict
import com.ureka.play4change.error.client.Forbidden.ResourceOwnershipViolation
import com.ureka.play4change.error.client.NotFound
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class TaskService(
    private val topicRepository: TopicRepository,
    private val topicModuleRepository: TopicModuleRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val taskInstanceRepository: TaskInstanceRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val userRepository: UserRepository,
    private val languageGatingService: LanguageGatingService,
    private val handleStruggleService: HandleStruggleService,
    private val peerReviewUseCase: PeerReviewUseCase,
    private val badgeIssuancePort: BadgeIssuancePort,
    private val registry: MeterRegistry
) : TaskUseCase {

    private val log = LoggerFactory.getLogger(TaskService::class.java)

    override fun getTodayTask(userId: String, topicId: String, timezone: String?): Either<AppError, TodayTaskResult> =
        either {
            val enrollment = ensureNotNull(enrollmentRepository.findByUserIdAndTopicId(userId, topicId)) {
                NotFound.ResourceNotFound("Enrollment", "$userId/$topicId")
            }

            val dayIndex = DayIndexCalculator.compute(enrollment.enrolledAt, timezone)

            // Existing assignment for today?
            val assignments = enrollmentRepository.findAssignmentsByEnrollmentId(enrollment.id)
            val existingPair = assignments.firstNotNullOfOrNull { a ->
                val template = taskTemplateRepository.findById(a.taskTemplateId)
                if (template?.dayIndex == dayIndex) Pair(a, template) else null
            }
            if (existingPair != null) return@either TodayTaskResult.Available(existingPair.first, existingPair.second)

            // Resolve which language variant to serve
            val user = ensureNotNull(userRepository.findById(userId)) {
                NotFound.ResourceNotFound("User", userId)
            }
            val topic = ensureNotNull(topicRepository.findById(topicId)) {
                NotFound.ResourceNotFound("Topic", topicId)
            }
            val modules = topicModuleRepository.findByTopicId(topicId)
            val module = ensureNotNull(modules.firstOrNull()) {
                NotFound.ResourceNotFound("TopicModule", topicId)
            }

            val gatingResult = languageGatingService.resolveTemplate(
                preferredLanguage = user.preferredLanguage,
                topicSourceLanguage = topic.language,
                moduleId = module.id,
                dayIndex = dayIndex
            )

            if (gatingResult is LanguageGatingResult.Pending) {
                return@either TodayTaskResult.GenerationPending(gatingResult.requestedLanguage)
            }

            val template = (gatingResult as LanguageGatingResult.Available).template
            val selectedInstance = selectTaskInstance(userId, template.id, enrollment.id)
            val effectiveOptionCount = selectedInstance?.options?.size ?: template.options?.size ?: 0
            val shuffledOrder = TaskShuffleSeed.shuffleOptions(
                effectiveOptionCount, userId, template.id, enrollment.id
            )
            val now = OffsetDateTime.now()

            val savedAssignment = enrollmentRepository.saveAssignment(
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
                    photoUrl = null,
                    taskInstanceId = selectedInstance?.id
                )
            )
            TodayTaskResult.Available(savedAssignment, template)
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

        val canonicalAnswer = resolveCorrectAnswer(
            assignment.taskInstanceId, assignment.taskTemplateId, template.correctAnswer
        )
        val isCorrect = originalIndex == canonicalAnswer
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

        triggerBadgeIssuance(isCorrect, command.userId, savedEnrollment.topicId, savedEnrollment.id)

        val submissionOutcome = when {
            isCorrect && isLate -> "late"
            isCorrect -> "correct"
            else -> "incorrect"
        }
        registry.counter(
            "task_submissions_total",
            "outcome", submissionOutcome,
            "task_type", "multiple_choice"
        ).increment()

        SubmitResult(
            assignment = savedAssignment,
            isCorrect = isCorrect,
            pointsAwarded = pointsAwarded,
            totalPoints = savedEnrollment.totalPointsEarned,
            streakDays = savedEnrollment.streakDays,
            struggleTriggered = struggleTriggered
        )
    }

    private fun selectTaskInstance(
        userId: String,
        templateId: String,
        enrollmentId: String
    ) = taskInstanceRepository.findByTaskTemplateId(templateId)
        .takeIf { it.isNotEmpty() }
        ?.let {
            val seed = TaskShuffleSeed.computeSeed(userId, templateId, enrollmentId)
            it[Math.floorMod(seed, it.size.toLong()).toInt()]
        }

    private fun triggerBadgeIssuance(isCorrect: Boolean, userId: String, topicId: String, enrollmentId: String) {
        if (isCorrect) badgeIssuancePort.issueBadge(userId, topicId, enrollmentId)
    }

    private fun resolveCorrectAnswer(instanceId: String?, templateId: String, fallback: Int?): Int? =
        instanceId
            ?.let { id -> taskInstanceRepository.findByTaskTemplateId(templateId).firstOrNull { it.id == id } }
            ?.correctAnswer
            ?: fallback

    override fun submitPhoto(command: SubmitPhotoCommand): Either<AppError, SubmitTodoResult> = either {
        val assignment = ensureNotNull(enrollmentRepository.findAssignmentById(command.assignmentId)) {
            NotFound.ResourceNotFound("TaskAssignment", command.assignmentId)
        }
        ensure(assignment.userId == command.userId) {
            ResourceOwnershipViolation("TaskAssignment")
        }
        ensure(assignment.status == AssignmentStatus.PENDING) {
            Conflict.ConcurrentModification
        }
        ensure(assignment.taskType == TaskType.TODO_ACTION) {
            BadRequest.InvalidField("assignmentId", "task is not a TODO_ACTION type")
        }
        ensure(command.photoUrl.isNotBlank()) {
            BadRequest.InvalidField("photoUrl", "must not be blank")
        }

        val savedAssignment = enrollmentRepository.saveAssignment(
            assignment.markPendingReview(command.photoUrl)
        )

        val enrollment = ensureNotNull(enrollmentRepository.findById(assignment.enrollmentId)) {
            NotFound.ResourceNotFound("Enrollment", assignment.enrollmentId)
        }

        // Delegate review selection through the PeerReviewUseCase interface
        val assignedReview = peerReviewUseCase.selectAndAssignReview(command.userId, enrollment.topicId)
            .fold(ifLeft = { null }, ifRight = { it })

        val assignedReviewPhotoUrl = assignedReview?.let { review ->
            enrollmentRepository.findAssignmentById(review.submissionAssignmentId)?.photoUrl
        }

        registry.counter(
            "task_submissions_total",
            "outcome", "pending_review",
            "task_type", "todo_action"
        ).increment()
        log.info("User {} submitted photo for assignment {}", command.userId, command.assignmentId)

        SubmitTodoResult(
            assignment = savedAssignment,
            assignedReview = assignedReview,
            assignedReviewPhotoUrl = assignedReviewPhotoUrl
        )
    }
}
