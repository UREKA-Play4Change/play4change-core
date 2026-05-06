package com.ureka.play4change.application.enrollment

import com.ureka.play4change.application.port.BadgeIssuancePort
import com.ureka.play4change.application.port.PeerReviewUseCase
import com.ureka.play4change.application.port.TodayTaskResult
import com.ureka.play4change.config.TaskDeliveryProperties
import com.ureka.play4change.config.TaskDeliveryStartupGuard
import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.Enrollment
import com.ureka.play4change.application.struggle.HandleStruggleService
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.enrollment.EnrollmentStatus
import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.domain.identity.AuthProvider
import com.ureka.play4change.domain.identity.User
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.domain.identity.UserRole
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskInstanceRepository
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TaskType
import com.ureka.play4change.domain.topic.TopicModule
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.env.Environment
import java.time.OffsetDateTime
import java.time.ZoneOffset

class TaskDeliveryRateTest {

    private val enrollmentRepository = mockk<EnrollmentRepository>()
    private val taskTemplateRepository = mockk<TaskTemplateRepository>(relaxed = true)
    private val taskInstanceRepository = mockk<TaskInstanceRepository>()
    private val userRepository = mockk<UserRepository>()
    private val topicRepository = mockk<TopicRepository>()
    private val topicModuleRepository = mockk<TopicModuleRepository>()
    private val languageGatingService = mockk<LanguageGatingService>()
    private val handleStruggleService = mockk<HandleStruggleService>(relaxed = true)
    private val peerReviewUseCase = mockk<PeerReviewUseCase>(relaxed = true)
    private val badgeIssuancePort = mockk<BadgeIssuancePort>(relaxed = true)
    private val registry = mockk<MeterRegistry>(relaxed = true)

    private fun makeService(devMode: Boolean, taskRateMinutes: Int = 1440): TaskService {
        val props = TaskDeliveryProperties().apply {
            this.devMode = devMode
            this.taskRateMinutes = taskRateMinutes
        }
        return TaskService(
            topicRepository, topicModuleRepository, taskTemplateRepository,
            taskInstanceRepository, enrollmentRepository, userRepository,
            languageGatingService, handleStruggleService, peerReviewUseCase,
            badgeIssuancePort, registry, props
        )
    }

    private val userId = "user-1"
    private val topicId = "topic-1"
    private val enrollmentId = "enrollment-1"
    private val templateId = "template-1"
    private val moduleId = "module-1"

    private val enrollment = Enrollment(
        id = enrollmentId,
        userId = userId,
        topicId = topicId,
        topicModuleId = moduleId,
        enrolledAt = OffsetDateTime.now(ZoneOffset.UTC),
        status = EnrollmentStatus.ACTIVE,
        currentDayIndex = 0,
        totalPointsEarned = 0,
        streakDays = 0,
        lastActivityAt = null
    )

    private fun makeTemplate(dayIndex: Int = 0) = TaskTemplate(
        id = templateId,
        moduleId = moduleId,
        dayIndex = dayIndex,
        poolIndex = 0,
        title = "Task $dayIndex",
        description = "Description",
        hint = null,
        taskType = TaskType.MULTIPLE_CHOICE,
        pointsReward = 20,
        options = listOf("A", "B", "C"),
        correctAnswer = 0,
        version = 1,
        isCurrent = true,
        supersededBy = null,
        embedding = null,
        language = "en",
        createdAt = OffsetDateTime.now(ZoneOffset.UTC)
    )

    private fun makeAssignment(
        status: AssignmentStatus = AssignmentStatus.SUBMITTED,
        submittedAt: OffsetDateTime? = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5),
        taskTemplateId: String = templateId
    ) = TaskAssignment(
        id = "assignment-1",
        enrollmentId = enrollmentId,
        userId = userId,
        taskTemplateId = taskTemplateId,
        taskTemplateVersion = 1,
        taskType = TaskType.MULTIPLE_CHOICE,
        assignedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10),
        dueAt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(24),
        submittedAt = submittedAt,
        status = status,
        selectedOption = 0,
        isCorrect = true,
        pointsAwarded = 20,
        optionOrder = listOf(0, 1, 2),
        wrongAttemptCount = 0,
        photoUrl = null
    )

    // ── Prod mode ─────────────────────────────────────────────────────────────

    @Test
    fun `prod mode - existing submitted assignment for today is returned unchanged`() {
        val service = makeService(devMode = false)
        val template = makeTemplate(dayIndex = 0)
        val submitted = makeAssignment(status = AssignmentStatus.SUBMITTED)

        every { enrollmentRepository.findByUserIdAndTopicId(userId, topicId) } returns enrollment
        every { enrollmentRepository.findAssignmentsByEnrollmentId(enrollmentId) } returns listOf(submitted)
        every { taskTemplateRepository.findById(templateId) } returns template

        val result = service.getTodayTask(userId, topicId, null).getOrNull()

        assertInstanceOf(TodayTaskResult.Available::class.java, result)
        assertEquals(submitted, (result as TodayTaskResult.Available).assignment)
    }

    // ── Dev mode rate check ───────────────────────────────────────────────────

    @Test
    fun `dev mode - before rate expires, next task is not available`() {
        val service = makeService(devMode = true)
        // submitted 1 minute ago — rate is 2 minutes, so not available yet
        val submitted = makeAssignment(
            status = AssignmentStatus.SUBMITTED,
            submittedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1)
        )

        every { enrollmentRepository.findByUserIdAndTopicId(userId, topicId) } returns enrollment
        every { enrollmentRepository.findAssignmentsByEnrollmentId(enrollmentId) } returns listOf(submitted)

        val result = service.getTodayTask(userId, topicId, null).getOrNull()

        assertInstanceOf(TodayTaskResult.NotAvailableYet::class.java, result)
    }

    @Test
    fun `dev mode - after rate expires, next task becomes available`() {
        val service = makeService(devMode = true)
        // submitted 3 minutes ago — rate is 2 minutes, so the next task is available
        val submitted = makeAssignment(
            status = AssignmentStatus.SUBMITTED,
            submittedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(3)
        )
        val nextTemplateId = "template-2"
        val nextTemplate = makeTemplate(dayIndex = 1).copy(id = nextTemplateId)
        val newAssignment = makeAssignment(
            status = AssignmentStatus.PENDING,
            submittedAt = null,
            taskTemplateId = nextTemplateId
        ).copy(id = "assignment-2")
        val user = User(
            id = userId, email = "a@b.com", name = null, avatarUrl = null,
            role = UserRole.USER, provider = AuthProvider.MAGIC_LINK, providerId = null,
            preferredLanguage = "en", audienceLevel = "BEGINNER",
            createdAt = OffsetDateTime.now(ZoneOffset.UTC)
        )
        val module = TopicModule(id = moduleId, topicId = topicId, orderIndex = 0, objective = "obj")

        every { enrollmentRepository.findByUserIdAndTopicId(userId, topicId) } returns enrollment
        every { enrollmentRepository.findAssignmentsByEnrollmentId(enrollmentId) } returns listOf(submitted)
        every { userRepository.findById(userId) } returns user
        every { topicRepository.findById(topicId) } returns mockk { every { language } returns "en" }
        every { topicModuleRepository.findByTopicId(topicId) } returns listOf(module)
        every {
            languageGatingService.resolveTemplate(
                preferredLanguage = "en",
                topicSourceLanguage = "en",
                moduleId = moduleId,
                dayIndex = 1
            )
        } returns LanguageGatingResult.Available(nextTemplate)
        every { taskInstanceRepository.findByTaskTemplateId(nextTemplateId) } returns emptyList()
        every { enrollmentRepository.saveAssignment(any()) } returns newAssignment

        val result = service.getTodayTask(userId, topicId, null).getOrNull()

        assertInstanceOf(TodayTaskResult.Available::class.java, result)
        assertEquals(newAssignment, (result as TodayTaskResult.Available).assignment)
    }

    @Test
    fun `dev mode - first task is always available with no prior submissions`() {
        val service = makeService(devMode = true)
        val template = makeTemplate(dayIndex = 0)
        val newAssignment = makeAssignment(status = AssignmentStatus.PENDING, submittedAt = null)
        val user = User(
            id = userId, email = "a@b.com", name = null, avatarUrl = null,
            role = UserRole.USER, provider = AuthProvider.MAGIC_LINK, providerId = null,
            preferredLanguage = "en", audienceLevel = "BEGINNER",
            createdAt = OffsetDateTime.now(ZoneOffset.UTC)
        )
        val module = TopicModule(id = moduleId, topicId = topicId, orderIndex = 0, objective = "obj")

        every { enrollmentRepository.findByUserIdAndTopicId(userId, topicId) } returns enrollment
        every { enrollmentRepository.findAssignmentsByEnrollmentId(enrollmentId) } returns emptyList()
        every { userRepository.findById(userId) } returns user
        every { topicRepository.findById(topicId) } returns mockk { every { language } returns "en" }
        every { topicModuleRepository.findByTopicId(topicId) } returns listOf(module)
        every {
            languageGatingService.resolveTemplate(
                preferredLanguage = "en",
                topicSourceLanguage = "en",
                moduleId = moduleId,
                dayIndex = 0
            )
        } returns LanguageGatingResult.Available(template)
        every { taskInstanceRepository.findByTaskTemplateId(templateId) } returns emptyList()
        every { enrollmentRepository.saveAssignment(any()) } returns newAssignment

        val result = service.getTodayTask(userId, topicId, null).getOrNull()

        assertInstanceOf(TodayTaskResult.Available::class.java, result)
    }

    // ── Startup guard ─────────────────────────────────────────────────────────

    @Test
    fun `dev mode enabled in prod profile throws IllegalStateException on startup`() {
        val props = TaskDeliveryProperties().apply {
            devMode = true
            taskRateMinutes = 1440
        }
        val env = mockk<Environment> {
            every { activeProfiles } returns arrayOf("prod")
        }
        val guard = TaskDeliveryStartupGuard(props, env)

        assertThrows<IllegalStateException> { guard.validate() }
    }

    @Test
    fun `taskRateMinutes=0 is rejected at startup with IllegalArgumentException`() {
        val props = TaskDeliveryProperties().apply {
            devMode = false
            taskRateMinutes = 0
        }
        val env = mockk<Environment> {
            every { activeProfiles } returns emptyArray()
        }
        val guard = TaskDeliveryStartupGuard(props, env)

        assertThrows<IllegalArgumentException> { guard.validate() }
    }
}
