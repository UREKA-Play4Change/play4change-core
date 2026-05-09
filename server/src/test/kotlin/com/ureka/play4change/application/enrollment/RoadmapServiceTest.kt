package com.ureka.play4change.application.enrollment

import com.ureka.play4change.application.port.RoadmapNodeStatus
import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.Enrollment
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.enrollment.EnrollmentStatus
import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.domain.peerreview.PeerReviewRepository
import com.ureka.play4change.domain.struggle.StruggleRepository
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TaskType
import com.ureka.play4change.domain.topic.TopicModule
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class RoadmapServiceTest {

    private val enrollmentRepository = mockk<EnrollmentRepository>()
    private val taskTemplateRepository = mockk<TaskTemplateRepository>()
    private val topicModuleRepository = mockk<TopicModuleRepository>()
    private val topicRepository = mockk<TopicRepository>()
    private val struggleRepository = mockk<StruggleRepository>()
    private val peerReviewRepository = mockk<PeerReviewRepository>()

    private val service = RoadmapService(
        topicRepository,
        topicModuleRepository,
        taskTemplateRepository,
        enrollmentRepository,
        struggleRepository,
        peerReviewRepository
    )

    private val userId = "user-1"
    private val topicId = "topic-1"
    private val enrollmentId = "enrollment-1"
    private val templateId = "template-1"
    private val moduleId = "module-1"

    // Enrolled today → DayIndexCalculator returns 0
    private val enrollment = Enrollment(
        id = enrollmentId,
        userId = userId,
        topicId = topicId,
        topicModuleId = moduleId,
        enrolledAt = OffsetDateTime.now(ZoneOffset.UTC),
        status = EnrollmentStatus.ACTIVE,
        currentDayIndex = 0,
        totalPointsEarned = 10,
        streakDays = 1,
        lastActivityAt = OffsetDateTime.now(ZoneOffset.UTC)
    )

    private val template = TaskTemplate(
        id = templateId,
        moduleId = moduleId,
        dayIndex = 0,
        poolIndex = 0,
        title = "Day 0 Task",
        description = "Description",
        hint = null,
        taskType = TaskType.MULTIPLE_CHOICE,
        pointsReward = 10,
        options = listOf("A", "B", "C"),
        correctAnswer = 0,
        version = 1,
        isCurrent = true,
        supersededBy = null,
        embedding = null,
        language = "en",
        createdAt = OffsetDateTime.now(ZoneOffset.UTC)
    )

    private val module = TopicModule(id = moduleId, topicId = topicId, orderIndex = 0, objective = "obj")

    private fun stubCommon(assignments: List<TaskAssignment>) {
        every { enrollmentRepository.findByUserIdAndTopicId(userId, topicId) } returns enrollment
        every { topicModuleRepository.findByTopicId(topicId) } returns listOf(module)
        every { taskTemplateRepository.findCurrentByModuleId(moduleId) } returns listOf(template)
        every { enrollmentRepository.findAssignmentsByEnrollmentId(enrollmentId) } returns assignments
        every { struggleRepository.findOpenByEnrollmentId(enrollmentId) } returns null
        every { peerReviewRepository.findPendingByReviewerUserId(userId) } returns emptyList()
    }

    @Test
    fun `given submitted assignment for today when getRoadmap called then node status is COMPLETED`() {
        val submitted = TaskAssignment(
            id = "assignment-1",
            enrollmentId = enrollmentId,
            userId = userId,
            taskTemplateId = templateId,
            taskTemplateVersion = 1,
            taskType = TaskType.MULTIPLE_CHOICE,
            assignedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(30),
            dueAt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(24),
            submittedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5),
            status = AssignmentStatus.SUBMITTED,
            selectedOption = 0,
            isCorrect = true,
            pointsAwarded = 10,
            optionOrder = listOf(0, 1, 2),
            wrongAttemptCount = 0,
            photoUrl = null
        )
        stubCommon(listOf(submitted))

        val nodes = service.getRoadmap(userId, topicId, null).getOrNull()!!
        val todayNode = nodes.first { it.dayIndex == 0 && !it.isAdaptive }

        assertEquals(RoadmapNodeStatus.COMPLETED, todayNode.status)
    }

    @Test
    fun `given pending assignment for today when getRoadmap called then node status is PENDING`() {
        val pending = TaskAssignment(
            id = "assignment-1",
            enrollmentId = enrollmentId,
            userId = userId,
            taskTemplateId = templateId,
            taskTemplateVersion = 1,
            taskType = TaskType.MULTIPLE_CHOICE,
            assignedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5),
            dueAt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(24),
            submittedAt = null,
            status = AssignmentStatus.PENDING,
            selectedOption = null,
            isCorrect = null,
            pointsAwarded = 0,
            optionOrder = listOf(0, 1, 2),
            wrongAttemptCount = 0,
            photoUrl = null
        )
        stubCommon(listOf(pending))

        val nodes = service.getRoadmap(userId, topicId, null).getOrNull()!!
        val todayNode = nodes.first { it.dayIndex == 0 && !it.isAdaptive }

        assertEquals(RoadmapNodeStatus.PENDING, todayNode.status)
    }

    @Test
    fun `given no assignment for today when getRoadmap called then node status is PENDING`() {
        stubCommon(emptyList())

        val nodes = service.getRoadmap(userId, topicId, null).getOrNull()!!
        val todayNode = nodes.first { it.dayIndex == 0 && !it.isAdaptive }

        assertEquals(RoadmapNodeStatus.PENDING, todayNode.status)
    }
}
