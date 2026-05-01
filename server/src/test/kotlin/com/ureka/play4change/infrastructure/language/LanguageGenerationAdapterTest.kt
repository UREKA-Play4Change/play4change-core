package com.ureka.play4change.infrastructure.language

import arrow.core.right
import com.ureka.play4change.domain.topic.AudienceLevel
import com.ureka.play4change.domain.topic.ContentSourceType
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.Topic
import com.ureka.play4change.domain.topic.TopicModule
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.domain.topic.TopicStatus
import com.ureka.play4change.model.GeneratedTask
import com.ureka.play4change.model.GenerationMetadata
import com.ureka.play4change.model.GenerationResult
import com.ureka.play4change.model.GenerationStatus
import com.ureka.play4change.port.TaskGenerationPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class LanguageGenerationAdapterTest {

    private val topicModuleRepository = mockk<TopicModuleRepository>()
    private val topicRepository = mockk<TopicRepository>()
    private val taskTemplateRepository = mockk<TaskTemplateRepository>()
    private val taskGenerationPort = mockk<TaskGenerationPort>()

    private val adapter = LanguageGenerationAdapter(
        topicModuleRepository,
        topicRepository,
        taskTemplateRepository,
        taskGenerationPort
    )

    private fun makeModule(topicId: String = "topic-1") = TopicModule(
        id = "module-1",
        topicId = topicId,
        orderIndex = 0,
        objective = "Learn about climate change"
    )

    private fun makeTopic(id: String = "topic-1") = Topic(
        id = id,
        title = "Climate Change",
        description = "An introduction to climate science",
        category = "science",
        contentSourceType = ContentSourceType.URL,
        contentSourceRef = "https://example.com/climate",
        rawExtractedText = "Climate change is the long-term shift in global temperatures.",
        taskCount = 5,
        subscriptionWindowDays = 30,
        expiresAt = OffsetDateTime.now().plusDays(30),
        audienceLevel = AudienceLevel.BEGINNER,
        language = "en",
        status = TopicStatus.ACTIVE,
        createdBy = "admin",
        createdAt = OffsetDateTime.now()
    )

    private fun makeGeneratedTask() = GeneratedTask(
        externalId = "ext-1",
        title = "What causes climate change?",
        description = "Which greenhouse gas is the primary driver of climate change?",
        hint = "Think about fossil fuels.",
        pointsReward = 20,
        embedding = FloatArray(1024) { 0.1f },
        status = GenerationStatus.SUCCESS,
        optionsJson = "[\"CO2\",\"N2\",\"O2\",\"Ar\"]",
        correctAnswerIndex = 0
    )

    @Test
    fun `given module and topic found when generation succeeds then saves template with requested language`() {
        val module = makeModule()
        val topic = makeTopic()
        val generatedTask = makeGeneratedTask()

        every { topicModuleRepository.findById("module-1") } returns module
        every { topicRepository.findById("topic-1") } returns topic
        coEvery { taskGenerationPort.generateTasks(any()) } returns GenerationResult(
            tasks = listOf(generatedTask),
            metadata = GenerationMetadata(1, 1, 0, 100L, 500L, "mistral")
        ).right()

        val savedSlot = slot<List<TaskTemplate>>()
        every { taskTemplateRepository.saveAll(capture(savedSlot)) } returns emptyList()

        adapter.triggerGeneration("module-1", 2, "pt-PT")

        verify(exactly = 1) { taskTemplateRepository.saveAll(any()) }
        val saved = savedSlot.captured.first()
        assertEquals("pt-PT", saved.language)
        assertEquals(2, saved.dayIndex)
        assertEquals("module-1", saved.moduleId)
    }

    @Test
    fun `given module not found when trigger called then no generation and no crash`() {
        every { topicModuleRepository.findById("module-99") } returns null

        adapter.triggerGeneration("module-99", 0, "pt-PT")

        coVerify(exactly = 0) { taskGenerationPort.generateTasks(any()) }
        verify(exactly = 0) { taskTemplateRepository.saveAll(any()) }
    }

    @Test
    fun `given topic not found when trigger called then no generation and no crash`() {
        val module = makeModule("topic-missing")
        every { topicModuleRepository.findById("module-1") } returns module
        every { topicRepository.findById("topic-missing") } returns null

        adapter.triggerGeneration("module-1", 0, "pt-PT")

        coVerify(exactly = 0) { taskGenerationPort.generateTasks(any()) }
        verify(exactly = 0) { taskTemplateRepository.saveAll(any()) }
    }

    @Test
    fun `given generation produces no successful tasks then nothing is saved`() {
        val module = makeModule()
        val topic = makeTopic()
        val failedTask = makeGeneratedTask().copy(status = GenerationStatus.FAILED)

        every { topicModuleRepository.findById("module-1") } returns module
        every { topicRepository.findById("topic-1") } returns topic
        coEvery { taskGenerationPort.generateTasks(any()) } returns GenerationResult(
            tasks = listOf(failedTask),
            metadata = GenerationMetadata(1, 0, 0, 50L, 200L, "mistral")
        ).right()

        adapter.triggerGeneration("module-1", 0, "pt-PT")

        verify(exactly = 0) { taskTemplateRepository.saveAll(any()) }
    }
}
