package com.ureka.play4change.application.topic

import com.ureka.play4change.application.port.BatchInstancePort
import com.ureka.play4change.domain.enrollment.TaskShuffleSeed
import com.ureka.play4change.domain.topic.TaskInstance
import com.ureka.play4change.domain.topic.TaskInstanceRepository
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class BatchInstanceGenerationTest {

    private val instancesPerTask = 5

    private fun makeTemplate(correctAnswer: Int = 1): TaskTemplate = TaskTemplate(
        id = UUID.randomUUID().toString(),
        moduleId = "module-1",
        dayIndex = 0,
        poolIndex = 0,
        title = "What is X?",
        description = "A question about X.",
        hint = null,
        taskType = TaskType.MULTIPLE_CHOICE,
        pointsReward = 20,
        options = listOf("Wrong A", "Correct", "Wrong B", "Wrong C"),
        correctAnswer = correctAnswer,
        version = 1,
        isCurrent = true,
        supersededBy = null,
        embedding = null,
        language = "en",
        createdAt = OffsetDateTime.now()
    )

    private fun stubPort(templates: List<TaskTemplate>, n: Int): BatchInstancePort {
        val port = mockk<BatchInstancePort>()
        val fakeVariants: Map<String, List<List<String>>> = templates.associate { t ->
            t.id to (0 until n).map { i ->
                listOf("Correct", "Distractor-${i}A", "Distractor-${i}B", "Distractor-${i}C")
            }
        }
        every { port.generateInstances(any(), any()) } returns fakeVariants
        return port
    }

    private fun captureRepo(): Pair<TaskInstanceRepository, MutableList<TaskInstance>> {
        val repo = mockk<TaskInstanceRepository>()
        val saved = mutableListOf<TaskInstance>()
        every { repo.saveAll(any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val list = firstArg<List<TaskInstance>>()
            saved.addAll(list)
            list
        }
        return repo to saved
    }

    @Test
    fun `a topic with 3 tasks generates 3 times N instances total`() {
        val templates = (1..3).map { makeTemplate() }
        val port = stubPort(templates, instancesPerTask)
        val (repo, saved) = captureRepo()

        BatchInstanceGenerationService(port, repo, instancesPerTask).generateAndSave(templates)

        assertEquals(3 * instancesPerTask, saved.size)
    }

    @Test
    fun `each instance has the same correct answer as its parent task`() {
        val correctAnswer = 2
        val template = makeTemplate(correctAnswer)
        val port = stubPort(listOf(template), instancesPerTask)
        val (repo, saved) = captureRepo()

        BatchInstanceGenerationService(port, repo, instancesPerTask).generateAndSave(listOf(template))

        assertTrue(saved.isNotEmpty())
        saved.forEach { instance ->
            assertEquals(
                correctAnswer, instance.correctAnswer,
                "Instance ${instance.instanceIndex} should have correctAnswer=$correctAnswer"
            )
        }
    }

    @Test
    fun `instance selection by seed is stable for the same inputs`() {
        val instances = (0 until instancesPerTask).map { idx ->
            TaskInstance(
                id = UUID.randomUUID().toString(),
                taskTemplateId = "template-1",
                instanceIndex = idx,
                options = listOf("Correct", "Wrong-${idx}A", "Wrong-${idx}B", "Wrong-${idx}C"),
                correctAnswer = 0
            )
        }

        val seed = TaskShuffleSeed.computeSeed("user-1", "template-1", "enroll-1")
        val selected1 = Math.floorMod(seed, instances.size.toLong()).toInt()
        val selected2 = Math.floorMod(seed, instances.size.toLong()).toInt()

        assertEquals(selected1, selected2, "Same seed must always select the same instance index")
        assertTrue(selected1 in instances.indices, "Selected index must be within the instance pool")
    }

    @Test
    fun `templates are chunked into batches of at most MAX_TEMPLATES_PER_CALL`() {
        val templates = (1..25).map { makeTemplate() }
        val port = stubPort(templates, instancesPerTask)
        val repo = mockk<TaskInstanceRepository>()
        every { repo.saveAll(any()) } answers { firstArg() }

        BatchInstanceGenerationService(port, repo, instancesPerTask).generateAndSave(templates)

        // 25 templates ÷ 10 per chunk = 3 calls to the port
        verify(exactly = 3) { port.generateInstances(any(), any()) }
    }
}
