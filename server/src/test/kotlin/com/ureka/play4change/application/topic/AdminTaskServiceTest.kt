package com.ureka.play4change.application.topic

import arrow.core.right
import com.ureka.play4change.application.port.TaskTemplateWithStats
import com.ureka.play4change.application.port.UpdateTaskCommand
import com.ureka.play4change.domain.struggle.AdaptiveTask
import com.ureka.play4change.domain.struggle.AdaptiveTaskAdminView
import com.ureka.play4change.domain.struggle.ErrorPattern
import com.ureka.play4change.domain.struggle.StruggleRepository
import com.ureka.play4change.domain.struggle.StruggleStatus
import com.ureka.play4change.domain.topic.AdminTaskStatsRepository
import com.ureka.play4change.domain.topic.TaskInstanceRepository
import com.ureka.play4change.domain.topic.TaskQuestionStats
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TaskType
import com.ureka.play4change.domain.topic.Topic
import com.ureka.play4change.domain.topic.TopicRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class AdminTaskServiceTest {

    private val topicRepository = mockk<TopicRepository>()
    private val taskTemplateRepository = mockk<TaskTemplateRepository>()
    private val statsRepository = mockk<AdminTaskStatsRepository>()
    private val struggleRepository = mockk<StruggleRepository>()
    private val taskInstanceRepository = mockk<TaskInstanceRepository>()
    private val batchInstanceGenerationService = mockk<BatchInstanceGenerationService>()

    private val service = AdminTaskService(
        topicRepository,
        taskTemplateRepository,
        statsRepository,
        struggleRepository,
        taskInstanceRepository,
        batchInstanceGenerationService
    )

    private val topicId = "topic-1"
    private val templateId = "template-1"

    private fun aTopic() = mockk<Topic>(relaxed = true)

    private fun aTemplate(id: String = templateId) = TaskTemplate(
        id = id,
        moduleId = "module-1",
        dayIndex = 0,
        poolIndex = 0,
        title = "What is the SDG?",
        description = "Choose the correct definition",
        hint = null,
        taskType = TaskType.MULTIPLE_CHOICE,
        pointsReward = 20,
        options = listOf("Option A", "Option B", "Option C", "Option D"),
        correctAnswer = 1,
        version = 1,
        isCurrent = true,
        supersededBy = null,
        embedding = null,
        language = "en",
        createdAt = OffsetDateTime.now()
    )

    private fun anAdaptiveView() = AdaptiveTaskAdminView(
        task = AdaptiveTask(
            id = "adaptive-1",
            struggleSessionId = "session-1",
            branchId = null,
            title = "Simpler version",
            description = "Choose carefully",
            hint = null,
            pointsReward = 10,
            orderIndex = 0,
            completedAt = null,
            isCorrect = null,
            options = listOf("A", "B"),
            correctAnswer = 0,
            selectedOption = null,
            optionOrder = listOf(0, 1)
        ),
        sessionId = "session-1",
        sessionStatus = StruggleStatus.OPEN,
        errorPattern = ErrorPattern.WRONG_CONCEPT,
        sessionDetectedAt = OffsetDateTime.now(),
        enrollmentId = "enrollment-1",
        originalTaskTemplateId = "template-1",
        originalTaskTitle = "Original task"
    )

    @Test
    fun `getTasksForTopic returns templates with zero stats when topic has no submissions`() {
        every { topicRepository.findById(topicId) } returns aTopic()
        every { taskTemplateRepository.findCurrentByTopicId(topicId) } returns listOf(aTemplate())
        every { statsRepository.getStatsByTemplateIds(listOf(templateId)) } returns emptyMap()

        val result = service.getTasksForTopic(topicId)

        assertTrue(result.isRight())
        val items = result.getOrNull()!!
        assertEquals(1, items.size)
        assertEquals(TaskQuestionStats.ZERO, items.first().stats)
    }

    @Test
    fun `getTasksForTopic returns per-question stats when assignments exist`() {
        val stats = TaskQuestionStats(totalAttempts = 10, successCount = 7, successRate = 0.7, avgPointsAwarded = 14.0)
        every { topicRepository.findById(topicId) } returns aTopic()
        every { taskTemplateRepository.findCurrentByTopicId(topicId) } returns listOf(aTemplate())
        every { statsRepository.getStatsByTemplateIds(listOf(templateId)) } returns mapOf(templateId to stats)

        val result = service.getTasksForTopic(topicId)

        val items = result.getOrNull()!!
        assertEquals(0.7, items.first().stats.successRate)
        assertEquals(10, items.first().stats.totalAttempts)
    }

    @Test
    fun `getTasksForTopic returns NotFound when topic does not exist`() {
        every { topicRepository.findById(topicId) } returns null

        val result = service.getTasksForTopic(topicId)

        assertTrue(result.isLeft())
    }

    @Test
    fun `getStruggleTasksForTopic returns adaptive task views for topic`() {
        every { topicRepository.findById(topicId) } returns aTopic()
        every { struggleRepository.findAdaptiveTasksByTopicId(topicId) } returns listOf(anAdaptiveView())

        val result = service.getStruggleTasksForTopic(topicId)

        assertTrue(result.isRight())
        assertEquals(1, result.getOrNull()!!.size)
    }

    @Test
    fun `getStruggleTasksForTopic returns empty list when no struggle sessions exist`() {
        every { topicRepository.findById(topicId) } returns aTopic()
        every { struggleRepository.findAdaptiveTasksByTopicId(topicId) } returns emptyList()

        val result = service.getStruggleTasksForTopic(topicId)

        assertTrue(result.isRight())
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `updateTask saves updated template and regenerates instances`() {
        val original = aTemplate()
        every { taskTemplateRepository.findById(templateId) } returns original
        every { taskTemplateRepository.save(any()) } answers { firstArg() }
        justRun { taskInstanceRepository.deleteByTaskTemplateId(templateId) }
        justRun { batchInstanceGenerationService.generateAndSave(any()) }

        val command = UpdateTaskCommand(
            title = "Updated title",
            description = "Updated description",
            hint = "New hint",
            options = listOf("X", "Y", "Z"),
            correctAnswer = 2
        )
        val result = service.updateTask(templateId, command)

        assertTrue(result.isRight())
        val updated = result.getOrNull()!!
        assertEquals("Updated title", updated.title)
        assertEquals(2, updated.version)
        verify(exactly = 1) { taskInstanceRepository.deleteByTaskTemplateId(templateId) }
        verify(exactly = 1) { batchInstanceGenerationService.generateAndSave(any()) }
    }

    @Test
    fun `updateTask returns NotFound when template does not exist`() {
        every { taskTemplateRepository.findById(templateId) } returns null

        val result = service.updateTask(templateId, UpdateTaskCommand("T", "D", null, null, null))

        assertTrue(result.isLeft())
    }

    @Test
    fun `updateTask returns BadRequest when title is blank`() {
        every { taskTemplateRepository.findById(templateId) } returns aTemplate()

        val result = service.updateTask(templateId, UpdateTaskCommand("", "D", null, null, null))

        assertTrue(result.isLeft())
    }

    @Test
    fun `updateTask returns BadRequest when correctAnswer is out of bounds for options`() {
        every { taskTemplateRepository.findById(templateId) } returns aTemplate()

        val result = service.updateTask(
            templateId,
            UpdateTaskCommand("Title", "Desc", null, listOf("A", "B"), correctAnswer = 5)
        )

        assertTrue(result.isLeft())
    }
}
