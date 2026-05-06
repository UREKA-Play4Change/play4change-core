package com.ureka.play4change.application.struggle

import com.ureka.play4change.application.enrollment.LanguageGatingService
import com.ureka.play4change.application.enrollment.TaskService
import com.ureka.play4change.application.port.BadgeIssuancePort
import com.ureka.play4change.application.port.PeerReviewUseCase
import com.ureka.play4change.application.port.SubmitAnswerCommand
import com.ureka.play4change.config.TaskDeliveryProperties
import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.Enrollment
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.enrollment.EnrollmentStatus
import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.domain.topic.TaskInstanceRepository
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TaskType
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class StruggleDetectionTest {

    private val enrollmentRepository = mockk<EnrollmentRepository>()
    private val taskTemplateRepository = mockk<TaskTemplateRepository>()
    private val taskInstanceRepository = mockk<TaskInstanceRepository>()
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val topicRepository = mockk<TopicRepository>(relaxed = true)
    private val topicModuleRepository = mockk<TopicModuleRepository>(relaxed = true)
    private val languageGatingService = mockk<LanguageGatingService>(relaxed = true)
    private val handleStruggleService = mockk<HandleStruggleService>(relaxed = true)
    private val peerReviewUseCase = mockk<PeerReviewUseCase>(relaxed = true)
    private val badgeIssuancePort = mockk<BadgeIssuancePort>(relaxed = true)
    private val registry = mockk<MeterRegistry>(relaxed = true) {
        every { counter(any(), *anyVararg()) } returns mockk<Counter>(relaxed = true)
    }

    private val service = TaskService(
        topicRepository, topicModuleRepository, taskTemplateRepository,
        taskInstanceRepository, enrollmentRepository, userRepository,
        languageGatingService, handleStruggleService, peerReviewUseCase,
        badgeIssuancePort, registry, TaskDeliveryProperties()
    )

    private val userId = "user-1"
    private val enrollmentId = "enrollment-1"
    private val templateId = "template-1"
    private val assignmentId = "assignment-1"

    private val enrollment = Enrollment(
        id = enrollmentId, userId = userId, topicId = "topic-1",
        topicModuleId = "module-1",
        enrolledAt = OffsetDateTime.now(ZoneOffset.UTC),
        status = EnrollmentStatus.ACTIVE,
        currentDayIndex = 0, totalPointsEarned = 0, streakDays = 0, lastActivityAt = null
    )

    private fun makeTemplate(correctAnswer: Int = 0) = TaskTemplate(
        id = templateId, moduleId = "module-1", dayIndex = 0, poolIndex = 0,
        title = "Test task", description = "Description", hint = null,
        taskType = TaskType.MULTIPLE_CHOICE, pointsReward = 20,
        options = listOf("A", "B", "C"), correctAnswer = correctAnswer,
        version = 1, isCurrent = true, supersededBy = null, embedding = null,
        language = "en", createdAt = OffsetDateTime.now(ZoneOffset.UTC)
    )

    private fun makeAssignment(wrongAttemptCount: Int, templateId: String = this.templateId) = TaskAssignment(
        id = assignmentId, enrollmentId = enrollmentId, userId = userId,
        taskTemplateId = templateId, taskTemplateVersion = 1,
        taskType = TaskType.MULTIPLE_CHOICE,
        assignedAt = OffsetDateTime.now(ZoneOffset.UTC),
        dueAt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(24),
        submittedAt = null, status = AssignmentStatus.PENDING,
        selectedOption = null, isCorrect = null, pointsAwarded = 0,
        optionOrder = listOf(0, 1, 2), wrongAttemptCount = wrongAttemptCount,
        photoUrl = null
    )

    private fun stubSave(assignment: TaskAssignment) {
        every { enrollmentRepository.findAssignmentById(assignmentId) } returns assignment
        every { enrollmentRepository.saveAssignment(any()) } answers { firstArg() }
        every { enrollmentRepository.findById(enrollmentId) } returns enrollment
        every { enrollmentRepository.save(any()) } answers { firstArg() }
        every { taskInstanceRepository.findByTaskTemplateId(templateId) } returns emptyList()
    }

    @Test
    fun `first wrong answer does not trigger struggle session creation`() {
        val assignment = makeAssignment(wrongAttemptCount = 0)
        val template = makeTemplate(correctAnswer = 0)
        stubSave(assignment)
        every { taskTemplateRepository.findById(templateId) } returns template

        // submit option index 1 (mapped from optionOrder[1] = 1, canonical answer = 0) → wrong
        service.submitAnswer(SubmitAnswerCommand(userId, assignmentId, selectedOption = 1, timezone = null))

        verify(exactly = 0) { handleStruggleService.triggerAsync(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `second consecutive wrong answer triggers struggle session creation`() {
        val assignment = makeAssignment(wrongAttemptCount = 1)
        val template = makeTemplate(correctAnswer = 0)
        stubSave(assignment)
        every { taskTemplateRepository.findById(templateId) } returns template

        // wrongAttemptCount=1 means this is the 2nd wrong → triggers struggle
        service.submitAnswer(SubmitAnswerCommand(userId, assignmentId, selectedOption = 1, timezone = null))

        verify(exactly = 1) { handleStruggleService.triggerAsync(enrollmentId, assignmentId, any(), template, userId) }
    }

    @Test
    fun `wrong answer on a different task does not trigger struggle for that new task`() {
        val otherTemplateId = "template-other"
        val otherAssignmentId = "assignment-other"
        val otherTemplate = makeTemplate(correctAnswer = 0).copy(id = otherTemplateId)
        // A brand-new assignment for a different task — wrongAttemptCount starts at 0
        val otherAssignment = makeAssignment(wrongAttemptCount = 0, templateId = otherTemplateId)
            .copy(id = otherAssignmentId)

        every { enrollmentRepository.findAssignmentById(otherAssignmentId) } returns otherAssignment
        every { enrollmentRepository.saveAssignment(any()) } answers { firstArg() }
        every { enrollmentRepository.findById(enrollmentId) } returns enrollment
        every { enrollmentRepository.save(any()) } answers { firstArg() }
        every { taskTemplateRepository.findById(otherTemplateId) } returns otherTemplate
        every { taskInstanceRepository.findByTaskTemplateId(otherTemplateId) } returns emptyList()

        // First wrong on the other task — should NOT trigger struggle
        service.submitAnswer(SubmitAnswerCommand(userId, otherAssignmentId, selectedOption = 1, timezone = null))

        verify(exactly = 0) { handleStruggleService.triggerAsync(any(), any(), any(), any(), any()) }
    }
}
