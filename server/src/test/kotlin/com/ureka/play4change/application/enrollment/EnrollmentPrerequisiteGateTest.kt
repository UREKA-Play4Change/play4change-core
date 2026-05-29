package com.ureka.play4change.application.enrollment

import com.ureka.play4change.application.port.EnrollCommand
import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.Enrollment
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.enrollment.EnrollmentStatus
import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.domain.topic.AudienceLevel
import com.ureka.play4change.domain.topic.ContentSourceType
import com.ureka.play4change.domain.topic.GenerationPhase
import com.ureka.play4change.domain.topic.PrerequisiteRepository
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TaskType
import com.ureka.play4change.domain.topic.Topic
import com.ureka.play4change.domain.topic.TopicModule
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.domain.topic.TopicStatus
import com.ureka.play4change.error.client.BadRequest
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class EnrollmentPrerequisiteGateTest {

    private val topicRepository = mockk<TopicRepository>()
    private val topicModuleRepository = mockk<TopicModuleRepository>()
    private val taskTemplateRepository = mockk<TaskTemplateRepository>()
    private val enrollmentRepository = mockk<EnrollmentRepository>()
    private val prerequisiteRepository = mockk<PrerequisiteRepository>()
    private val registry: MeterRegistry = SimpleMeterRegistry()

    private val service = EnrollmentService(
        topicRepository,
        topicModuleRepository,
        taskTemplateRepository,
        enrollmentRepository,
        prerequisiteRepository,
        registry
    )

    private val userId = "user-1"
    private val topicBId = "topic-b"
    private val topicAId = "topic-a"

    private fun activeTopic(id: String) = Topic(
        id = id,
        title = "Topic $id",
        description = "Description",
        category = "DIGITAL",
        contentSourceType = ContentSourceType.URL,
        contentSourceRef = "ref",
        rawExtractedText = null,
        taskCount = 5,
        subscriptionWindowDays = 7,
        expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(30),
        audienceLevel = AudienceLevel.BEGINNER,
        language = "en",
        status = TopicStatus.ACTIVE,
        createdBy = "admin-1",
        createdAt = OffsetDateTime.now(ZoneOffset.UTC),
        currentPhase = GenerationPhase.INDEXING,
        phaseUpdatedAt = OffsetDateTime.now(ZoneOffset.UTC)
    )

    private fun completedEnrollment(topicId: String) = Enrollment(
        id = "enrollment-$topicId",
        userId = userId,
        topicId = topicId,
        topicModuleId = "module-$topicId",
        enrolledAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(10),
        status = EnrollmentStatus.COMPLETED,
        currentDayIndex = 5,
        totalPointsEarned = 50,
        streakDays = 5,
        lastActivityAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)
    )

    private fun aModule(topicId: String) = TopicModule(
        id = "module-$topicId",
        topicId = topicId,
        orderIndex = 0,
        objective = "obj"
    )

    private fun aTemplate(moduleId: String) = TaskTemplate(
        id = "template-$moduleId",
        moduleId = moduleId,
        dayIndex = 0,
        poolIndex = 0,
        title = "What is SDG?",
        description = "Choose the definition",
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

    private fun stubEnrollSuccess(topicId: String) {
        val module = aModule(topicId)
        every { topicModuleRepository.findByTopicId(topicId) } returns listOf(module)
        every { taskTemplateRepository.findCurrentByModuleId(module.id) } returns listOf(aTemplate(module.id))
        every { enrollmentRepository.findByUserIdAndTopicId(userId, topicId) } returns null
        every { enrollmentRepository.save(any()) } answers { firstArg() }
        every { enrollmentRepository.saveAssignment(any()) } returns mockk<TaskAssignment>()
    }

    // -------------------------------------------------------------------------
    // Gate blocks enrollment when prerequisite is not completed
    // -------------------------------------------------------------------------

    @Test
    fun `enroll returns 400 when prerequisite topic is not completed`() {
        every { topicRepository.findById(topicBId) } returns activeTopic(topicBId)
        every { prerequisiteRepository.findPrerequisitesByTopicId(topicBId) } returns listOf(topicAId)
        every { enrollmentRepository.findCompletedByUserId(userId) } returns emptyList()

        val result = service.enroll(EnrollCommand(userId, topicBId))

        assertTrue(result.isLeft())
        result.onLeft {
            assertTrue(it is BadRequest.InvalidField)
            val field = it as BadRequest.InvalidField
            assertTrue(field.reason.contains("prerequisites"))
        }
    }

    @Test
    fun `enroll returns 400 when only some prerequisites are completed`() {
        val topicCId = "topic-c"
        every { topicRepository.findById(topicBId) } returns activeTopic(topicBId)
        every { prerequisiteRepository.findPrerequisitesByTopicId(topicBId) } returns listOf(topicAId, topicCId)
        // user completed A but not C
        every { enrollmentRepository.findCompletedByUserId(userId) } returns listOf(completedEnrollment(topicAId))

        val result = service.enroll(EnrollCommand(userId, topicBId))

        assertTrue(result.isLeft())
        result.onLeft { assertTrue(it is BadRequest.InvalidField) }
    }

    // -------------------------------------------------------------------------
    // Gate allows enrollment when all prerequisites are completed
    // -------------------------------------------------------------------------

    @Test
    fun `enroll succeeds when all prerequisites are completed`() {
        every { topicRepository.findById(topicBId) } returns activeTopic(topicBId)
        every { prerequisiteRepository.findPrerequisitesByTopicId(topicBId) } returns listOf(topicAId)
        every { enrollmentRepository.findCompletedByUserId(userId) } returns listOf(completedEnrollment(topicAId))
        stubEnrollSuccess(topicBId)

        val result = service.enroll(EnrollCommand(userId, topicBId))

        assertTrue(result.isRight())
    }

    // -------------------------------------------------------------------------
    // Gate is skipped when topic has no prerequisites
    // -------------------------------------------------------------------------

    @Test
    fun `enroll succeeds when topic has no prerequisites`() {
        every { topicRepository.findById(topicBId) } returns activeTopic(topicBId)
        every { prerequisiteRepository.findPrerequisitesByTopicId(topicBId) } returns emptyList()
        stubEnrollSuccess(topicBId)

        val result = service.enroll(EnrollCommand(userId, topicBId))

        assertTrue(result.isRight())
    }

    // -------------------------------------------------------------------------
    // getCompletedTopicIds
    // -------------------------------------------------------------------------

    @Test
    fun `getCompletedTopicIds returns set of completed topic ids`() {
        every { enrollmentRepository.findCompletedByUserId(userId) } returns listOf(
            completedEnrollment(topicAId),
            completedEnrollment("topic-c")
        )

        val result = service.getCompletedTopicIds(userId)

        assertTrue(result.contains(topicAId))
        assertTrue(result.contains("topic-c"))
        assertTrue(result.size == 2)
    }

    @Test
    fun `getCompletedTopicIds returns empty set when user has no completed enrollments`() {
        every { enrollmentRepository.findCompletedByUserId(userId) } returns emptyList()

        val result = service.getCompletedTopicIds(userId)

        assertTrue(result.isEmpty())
    }
}
