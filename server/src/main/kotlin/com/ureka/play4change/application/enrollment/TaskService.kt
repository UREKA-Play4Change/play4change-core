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
import com.ureka.play4change.config.TaskDeliveryProperties
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.application.struggle.ErrorPatternClassifier
import com.ureka.play4change.application.struggle.HandleStruggleService
import com.ureka.play4change.domain.explanation.ExplanationRepository
import com.ureka.play4change.domain.struggle.StruggleRepository
import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.enrollment.EnrollmentStatus
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
import java.time.temporal.ChronoUnit
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
    private val struggleRepository: StruggleRepository,
    private val explanationRepository: ExplanationRepository,
    private val peerReviewUseCase: PeerReviewUseCase,
    private val badgeIssuancePort: BadgeIssuancePort,
    private val registry: MeterRegistry,
    private val taskDeliveryProperties: TaskDeliveryProperties
) : TaskUseCase {

    private val log = LoggerFactory.getLogger(TaskService::class.java)

    @Suppress("LongMethod") // multi-branch prod/dev task delivery logic — splitting would obscure the flow
    override fun getTodayTask(userId: String, topicId: String, timezone: String?): Either<AppError, TodayTaskResult> =
        either {
            val enrollment = ensureNotNull(enrollmentRepository.findByUserIdAndTopicId(userId, topicId)) {
                NotFound.ResourceNotFound("Enrollment", "$userId/$topicId")
            }

            val assignments = enrollmentRepository.findAssignmentsByEnrollmentId(enrollment.id)

            // Re-route to active explanation session (explanation mode takes precedence over struggle)
            val activeExplanation = explanationRepository.findActiveByEnrollmentId(enrollment.id)
            if (activeExplanation != null) {
                return@either TodayTaskResult.ExplanationActive(enrollment.id, activeExplanation.id)
            }

            // Re-route to open struggle session if one exists (user navigated away mid-struggle)
            if (struggleRepository.findOpenByEnrollmentId(enrollment.id) != null) {
                return@either TodayTaskResult.StruggleOpen(enrollment.id)
            }

            val dayIndex: Int
            if (taskDeliveryProperties.devMode) {
                // Dev mode: return the current PENDING assignment if one exists
                val pendingPair = assignments.firstNotNullOfOrNull { a ->
                    if (a.status == AssignmentStatus.PENDING) {
                        taskTemplateRepository.findById(a.taskTemplateId)?.let { Pair(a, it) }
                    } else null
                }
                if (pendingPair != null) return@either TodayTaskResult.Available(pendingPair.first, pendingPair.second)

                // Rate check: last submission must be >= effectiveRateSeconds ago
                val lastSubmittedAt = assignments.mapNotNull { it.submittedAt }.maxOrNull()
                if (lastSubmittedAt != null) {
                    val rateSeconds = taskDeliveryProperties.effectiveRateSeconds()
                    val secondsSince = ChronoUnit.SECONDS.between(lastSubmittedAt, OffsetDateTime.now())
                    if (secondsSince < rateSeconds) {
                        return@either TodayTaskResult.NotAvailableYet(lastSubmittedAt.plusSeconds(rateSeconds))
                    }
                }

                // Next day index = count of assignments that have been submitted
                dayIndex = assignments.count { it.submittedAt != null }
                val taskCount = ensureNotNull(topicRepository.findById(topicId)) {
                    NotFound.ResourceNotFound("Topic", topicId)
                }.taskCount
                if (dayIndex >= taskCount) {
                    return@either TodayTaskResult.NotAvailableYet(OffsetDateTime.now().plusYears(100))
                }
            } else {
                // Prod mode: serve any reset PENDING task (e.g. from a resolved struggle) before
                // doing calendar-day logic — the reset assignment may be from a prior calendar day.
                val resetPending = assignments.firstNotNullOfOrNull { a ->
                    if (a.status == AssignmentStatus.PENDING) {
                        taskTemplateRepository.findById(a.taskTemplateId)?.let { Pair(a, it) }
                    } else null
                }
                if (resetPending != null) return@either TodayTaskResult.Available(resetPending.first, resetPending.second)

                // Calendar-day-based unlock at midnight in user's timezone
                dayIndex = DayIndexCalculator.compute(enrollment.enrolledAt, timezone)

                val existingPair = assignments.firstNotNullOfOrNull { a ->
                    val template = taskTemplateRepository.findById(a.taskTemplateId)
                    if (template?.dayIndex == dayIndex) Pair(a, template) else null
                }
                if (existingPair != null) {
                    // existingPair was already SUBMITTED (we checked PENDING above); nothing left today
                    return@either TodayTaskResult.NotAvailableYet(
                        DayIndexCalculator.startOfTomorrow(timezone)
                    )
                }
            }

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
                    dueAt = DayIndexCalculator.startOfTomorrow(timezone),
                    submittedAt = null,
                    status = AssignmentStatus.PENDING,
                    selectedOption = null,
                    isCorrect = null,
                    pointsAwarded = 0,
                    optionOrder = shuffledOrder,
                    wrongAttemptCount = 0,
                    photoUrl = null,
                    taskInstanceId = selectedInstance?.id,
                    correctAnswerIndex = selectedInstance?.correctAnswer ?: template.correctAnswer
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

        val canonicalAnswer = assignment.correctAnswerIndex
            ?: resolveCorrectAnswer(assignment.taskInstanceId, assignment.taskTemplateId, template.correctAnswer)
        val isCorrect = originalIndex == canonicalAnswer
        val now = OffsetDateTime.now()
        val isLate = now.isAfter(assignment.dueAt)

        val pointsAwarded = when {
            !isCorrect -> 0
            !isLate -> template.pointsReward
            else -> {
                val hoursLate = ChronoUnit.HOURS.between(assignment.dueAt, now).toInt()
                when {
                    hoursLate <= 24 -> (template.pointsReward * 0.75).toInt()
                    hoursLate <= 72 -> template.pointsReward / 2
                    else -> (template.pointsReward * 0.25).toInt().coerceAtLeast(1)
                }
            }
        }

        var struggleTriggered = false

        val updatedAssignment = if (isCorrect) {
            assignment.markSubmitted(
                isCorrect = true,
                pointsAwarded = pointsAwarded,
                selectedOption = command.selectedOption
            )
        } else {
            // Wrong answer — immediately final, trigger struggle
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
            assignment.incrementWrongAttempts().markSubmitted(
                isCorrect = false,
                pointsAwarded = 0,
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
        } else {
            // Wrong answer: keep the streak intact — completing the struggle path will increment it
            enrollment
        }

        val savedEnrollment = enrollmentRepository.save(updatedEnrollment)

        triggerBadgeIssuance(isCorrect, command.userId, savedEnrollment.topicId, savedEnrollment.id)

        // Transition enrollment to COMPLETED when all tasks have been submitted
        if (updatedAssignment.submittedAt != null && savedEnrollment.status == EnrollmentStatus.ACTIVE) {
            val topic = topicRepository.findById(savedEnrollment.topicId)
            if (topic != null) {
                val submittedCount = enrollmentRepository.findAssignmentsByEnrollmentId(savedEnrollment.id)
                    .count { it.submittedAt != null }
                if (submittedCount >= topic.taskCount) {
                    enrollmentRepository.save(savedEnrollment.complete())
                    log.info("Enrollment {} completed for user {} on topic {}", savedEnrollment.id, command.userId, savedEnrollment.topicId)
                    // Trigger badge issuance on completion in case the last answer was wrong
                    // (the earlier triggerBadgeIssuance only fires for correct answers)
                    if (!isCorrect) badgeIssuancePort.issueBadge(command.userId, savedEnrollment.topicId, savedEnrollment.id)
                }
            }
        }

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

        registry.counter(
            "tasks_submitted_total",
            "result", if (isCorrect) "correct" else "incorrect",
            "topic_id", savedEnrollment.topicId
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
