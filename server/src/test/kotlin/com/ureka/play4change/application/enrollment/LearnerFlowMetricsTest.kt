package com.ureka.play4change.application.enrollment

import com.ureka.play4change.application.port.PeerReviewUseCase
import com.ureka.play4change.application.port.BadgeIssuancePort
import com.ureka.play4change.application.port.SubmitAnswerCommand
import com.ureka.play4change.application.port.SubmitVerdictCommand
import com.ureka.play4change.application.peerreview.PeerReviewService
import com.ureka.play4change.application.struggle.HandleStruggleService
import com.ureka.play4change.config.TaskDeliveryProperties
import com.ureka.play4change.domain.explanation.ExplanationRepository
import com.ureka.play4change.domain.struggle.StruggleRepository
import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.Enrollment
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.enrollment.EnrollmentStatus
import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.domain.peerreview.PeerReview
import com.ureka.play4change.domain.peerreview.PeerReviewRepository
import com.ureka.play4change.domain.peerreview.ReviewVerdict
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TaskType
import com.ureka.play4change.domain.topic.TaskInstanceRepository
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class LearnerFlowMetricsTest {

    // ── shared fixtures ──────────────────────────────────────────────────────

    private val topicId = "topic-metrics-1"
    private val userId = "user-metrics-1"
    private val enrollmentId = "enrollment-metrics-1"
    private val templateId = "template-metrics-1"
    private val assignmentId = "assignment-metrics-1"

    private val enrollment = Enrollment(
        id = enrollmentId,
        userId = userId,
        topicId = topicId,
        topicModuleId = "module-1",
        enrolledAt = OffsetDateTime.now(),
        status = EnrollmentStatus.ACTIVE,
        currentDayIndex = 0,
        totalPointsEarned = 0,
        streakDays = 0,
        lastActivityAt = null
    )

    private val template = TaskTemplate(
        id = templateId,
        moduleId = "module-1",
        dayIndex = 0,
        poolIndex = 0,
        title = "Test Question",
        description = "What is sustainability?",
        hint = null,
        taskType = TaskType.MULTIPLE_CHOICE,
        pointsReward = 20,
        options = listOf("A", "B", "C", "D"),
        correctAnswer = 0,
        version = 1,
        isCurrent = true,
        supersededBy = null,
        embedding = null,
        createdAt = OffsetDateTime.now()
    )

    private val now = OffsetDateTime.now()

    private val assignment = TaskAssignment(
        id = assignmentId,
        enrollmentId = enrollmentId,
        userId = userId,
        taskTemplateId = templateId,
        taskTemplateVersion = 1,
        taskType = TaskType.MULTIPLE_CHOICE,
        assignedAt = now.minusMinutes(5),
        dueAt = now.plusHours(23),
        submittedAt = null,
        status = AssignmentStatus.PENDING,
        selectedOption = null,
        isCorrect = null,
        pointsAwarded = 0,
        optionOrder = listOf(0, 1, 2, 3),
        wrongAttemptCount = 0,
        photoUrl = null,
        taskInstanceId = null
    )

    // ── Task 1: tasks_submitted_total counter ────────────────────────────────

    @Test
    fun `submitAnswer records tasks_submitted_total with result=correct and topic_id`() {
        val meterRegistry = SimpleMeterRegistry()

        val enrollmentRepo = mockk<EnrollmentRepository>()
        every { enrollmentRepo.findAssignmentById(assignmentId) } returns assignment
        every { enrollmentRepo.findById(enrollmentId) } returns enrollment
        every { enrollmentRepo.saveAssignment(any()) } answers { firstArg() }
        every { enrollmentRepo.save(any()) } answers { firstArg() }
        every { enrollmentRepo.findAssignmentsByEnrollmentId(enrollmentId) } returns listOf(
            assignment.copy(submittedAt = now, status = AssignmentStatus.SUBMITTED)
        )

        val templateRepo = mockk<TaskTemplateRepository>()
        every { templateRepo.findById(templateId) } returns template

        val topicRepo = mockk<TopicRepository>()
        every { topicRepo.findById(any()) } returns null
        val instanceRepo = mockk<TaskInstanceRepository>()
        every { instanceRepo.findByTaskTemplateId(any()) } returns emptyList()

        val taskService = TaskService(
            topicRepository = topicRepo,
            topicModuleRepository = mockk(relaxed = true),
            taskTemplateRepository = templateRepo,
            taskInstanceRepository = instanceRepo,
            enrollmentRepository = enrollmentRepo,
            userRepository = mockk(relaxed = true),
            languageGatingService = mockk(relaxed = true),
            handleStruggleService = mockk(relaxed = true),
            struggleRepository = mockk<StruggleRepository>(relaxed = true),
            explanationRepository = mockk<ExplanationRepository>(relaxed = true),
            peerReviewUseCase = mockk(relaxed = true),
            badgeIssuancePort = mockk<BadgeIssuancePort>(relaxed = true),
            registry = meterRegistry,
            taskDeliveryProperties = TaskDeliveryProperties()
        )

        val result = taskService.submitAnswer(SubmitAnswerCommand(userId, assignmentId, 0, null))

        assertTrue(result.isRight()) { "Expected Right but got: $result" }

        val counter = meterRegistry.find("tasks_submitted_total")
            .tags("result", "correct", "topic_id", topicId)
            .counter()

        assertNotNull(counter) { "Expected tasks.submitted.total with result=correct and topic_id=$topicId" }
        assertTrue(counter!!.count() > 0)
    }

    @Test
    fun `submitAnswer records tasks_submitted_total with result=incorrect when answer is wrong`() {
        val meterRegistry = SimpleMeterRegistry()

        val enrollmentRepo = mockk<EnrollmentRepository>()
        // assignment with one prior wrong attempt so this 2nd wrong triggers final submission
        val assignmentWithWrongAttempt = assignment.copy(wrongAttemptCount = 1)
        every { enrollmentRepo.findAssignmentById(assignmentId) } returns assignmentWithWrongAttempt
        every { enrollmentRepo.findById(enrollmentId) } returns enrollment
        every { enrollmentRepo.saveAssignment(any()) } answers { firstArg() }
        every { enrollmentRepo.save(any()) } answers { firstArg() }
        every { enrollmentRepo.findAssignmentsByEnrollmentId(enrollmentId) } returns emptyList()

        val templateRepo = mockk<TaskTemplateRepository>()
        every { templateRepo.findById(templateId) } returns template

        val handleStruggle = mockk<HandleStruggleService>(relaxed = true)

        val topicRepo2 = mockk<TopicRepository>()
        every { topicRepo2.findById(any()) } returns null
        val instanceRepo2 = mockk<TaskInstanceRepository>()
        every { instanceRepo2.findByTaskTemplateId(any()) } returns emptyList()

        val taskService = TaskService(
            topicRepository = topicRepo2,
            topicModuleRepository = mockk(relaxed = true),
            taskTemplateRepository = templateRepo,
            taskInstanceRepository = instanceRepo2,
            enrollmentRepository = enrollmentRepo,
            userRepository = mockk(relaxed = true),
            languageGatingService = mockk(relaxed = true),
            handleStruggleService = handleStruggle,
            struggleRepository = mockk<StruggleRepository>(relaxed = true),
            explanationRepository = mockk<ExplanationRepository>(relaxed = true),
            peerReviewUseCase = mockk(relaxed = true),
            badgeIssuancePort = mockk<BadgeIssuancePort>(relaxed = true),
            registry = meterRegistry,
            taskDeliveryProperties = TaskDeliveryProperties()
        )

        // selectedOption=3 maps to optionOrder[3]=3, which != correctAnswer(0) → incorrect
        taskService.submitAnswer(SubmitAnswerCommand(userId, assignmentId, 3, null))

        val counter = meterRegistry.find("tasks_submitted_total")
            .tags("result", "incorrect", "topic_id", topicId)
            .counter()

        assertNotNull(counter) { "Expected tasks.submitted.total with result=incorrect and topic_id=$topicId" }
        assertTrue(counter!!.count() > 0)
    }

    // ── Task 2: reviews_verdicts_submitted_total counter ─────────────────────

    @Test
    fun `submitVerdict records reviews_verdicts_submitted_total with verdict and topic_id`() {
        val meterRegistry = SimpleMeterRegistry()

        val reviewId = "review-metrics-1"
        val submissionAssignmentId = "submission-assignment-1"

        val peerReview = PeerReview(
            id = reviewId,
            submissionAssignmentId = submissionAssignmentId,
            reviewerUserId = userId,
            verdict = null,
            comment = null,
            assignedAt = now.minusMinutes(10),
            expiresAt = now.plusDays(1),
            reviewedAt = null
        )

        val submissionAssignment = assignment.copy(
            id = submissionAssignmentId,
            userId = "submitter-1",
            status = AssignmentStatus.PENDING,
            submittedAt = null
        )

        val submitterEnrollment = enrollment.copy(userId = "submitter-1")

        val reviewRepo = mockk<PeerReviewRepository>()
        every { reviewRepo.findById(reviewId) } returns peerReview
        every { reviewRepo.save(any()) } answers { firstArg() }
        every { reviewRepo.findBySubmissionAssignmentId(submissionAssignmentId) } returns listOf(
            peerReview.copy(verdict = ReviewVerdict.CORRECT)
        )

        val enrollmentRepo = mockk<EnrollmentRepository>()
        every { enrollmentRepo.findAssignmentById(submissionAssignmentId) } returns submissionAssignment
        every { enrollmentRepo.findById(enrollmentId) } returns submitterEnrollment

        val templateRepo = mockk<TaskTemplateRepository>()

        val peerReviewService = PeerReviewService(
            peerReviewRepository = reviewRepo,
            enrollmentRepository = enrollmentRepo,
            taskTemplateRepository = templateRepo,
            registry = meterRegistry
        )

        peerReviewService.submitVerdict(
            SubmitVerdictCommand(userId, reviewId, ReviewVerdict.CORRECT, null)
        )

        val counter = meterRegistry.find("reviews_verdicts_submitted_total")
            .tags("verdict", "correct", "topic_id", topicId)
            .counter()

        assertNotNull(counter) { "Expected reviews.verdicts.submitted.total with verdict=correct and topic_id=$topicId" }
        assertTrue(counter!!.count() > 0)
    }
}
