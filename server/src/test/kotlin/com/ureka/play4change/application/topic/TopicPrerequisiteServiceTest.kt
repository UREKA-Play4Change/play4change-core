package com.ureka.play4change.application.topic

import com.ureka.play4change.application.port.ContentExtractorPort
import com.ureka.play4change.application.port.FileStoragePort
import com.ureka.play4change.domain.topic.AudienceLevel
import com.ureka.play4change.domain.topic.ContentSourceType
import com.ureka.play4change.domain.topic.GenerationPhase
import com.ureka.play4change.domain.topic.PrerequisiteRepository
import com.ureka.play4change.domain.topic.Topic
import com.ureka.play4change.domain.topic.TopicPhaseLogRepository
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.domain.topic.TopicStatsRepository
import com.ureka.play4change.domain.topic.TopicStatus
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.error.client.NotFound
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class TopicPrerequisiteServiceTest {

    private val topicRepository = mockk<TopicRepository>()
    private val prerequisiteRepository = mockk<PrerequisiteRepository>()
    private val fileStoragePort = mockk<FileStoragePort>()
    private val contentExtractorPort = mockk<ContentExtractorPort>()
    private val orchestrator = mockk<TaskGenerationOrchestrator>()
    private val phaseTransitionService = mockk<PhaseTransitionService>()
    private val phaseLogRepository = mockk<TopicPhaseLogRepository>()
    private val statsRepository = mockk<TopicStatsRepository>()

    private val service = TopicManagementService(
        topicRepository,
        fileStoragePort,
        contentExtractorPort,
        orchestrator,
        phaseTransitionService,
        phaseLogRepository,
        statsRepository,
        prerequisiteRepository
    )

    private fun aTopic(id: String, title: String = "Topic $id") = Topic(
        id = id,
        title = title,
        description = "Description",
        category = "DIGITAL",
        contentSourceType = ContentSourceType.URL,
        contentSourceRef = "ref",
        rawExtractedText = null,
        taskCount = 5,
        expiresAt = OffsetDateTime.now().plusDays(30),
        audienceLevel = AudienceLevel.BEGINNER,
        language = "en",
        status = TopicStatus.ACTIVE,
        createdBy = "admin-1",
        createdAt = OffsetDateTime.now(),
        currentPhase = GenerationPhase.INDEXING,
        phaseUpdatedAt = OffsetDateTime.now()
    )

    // -------------------------------------------------------------------------
    // getPrerequisites
    // -------------------------------------------------------------------------

    @Test
    fun `getPrerequisites returns 404 when topic does not exist`() {
        every { topicRepository.findById("missing") } returns null

        val result = service.getPrerequisites("missing")

        assertTrue(result.isLeft())
        result.onLeft { assertTrue(it is NotFound.ResourceNotFound) }
    }

    @Test
    fun `getPrerequisites returns empty list when no prerequisites are set`() {
        every { topicRepository.findById("topic-1") } returns aTopic("topic-1")
        every { prerequisiteRepository.findPrerequisitesByTopicId("topic-1") } returns emptyList()

        val result = service.getPrerequisites("topic-1")

        assertTrue(result.isRight())
        assertEquals(emptyList<Topic>(), result.getOrNull())
    }

    @Test
    fun `getPrerequisites returns resolved topic list`() {
        every { topicRepository.findById("topic-b") } returns aTopic("topic-b")
        every { topicRepository.findById("topic-a") } returns aTopic("topic-a")
        every { prerequisiteRepository.findPrerequisitesByTopicId("topic-b") } returns listOf("topic-a")

        val result = service.getPrerequisites("topic-b")

        assertTrue(result.isRight())
        val topics = result.getOrNull()!!
        assertEquals(1, topics.size)
        assertEquals("topic-a", topics[0].id)
    }

    // -------------------------------------------------------------------------
    // setPrerequisites — validation
    // -------------------------------------------------------------------------

    @Test
    fun `setPrerequisites returns 404 when topic does not exist`() {
        every { topicRepository.findById("missing") } returns null

        val result = service.setPrerequisites("missing", listOf("topic-a"))

        assertTrue(result.isLeft())
        result.onLeft { assertTrue(it is NotFound.ResourceNotFound) }
    }

    @Test
    fun `setPrerequisites returns 400 when topic is its own prerequisite`() {
        every { topicRepository.findById("topic-a") } returns aTopic("topic-a")

        val result = service.setPrerequisites("topic-a", listOf("topic-a"))

        assertTrue(result.isLeft())
        result.onLeft { assertTrue(it is BadRequest.InvalidField) }
    }

    @Test
    fun `setPrerequisites returns 404 when a prerequisite topic does not exist`() {
        every { topicRepository.findById("topic-b") } returns aTopic("topic-b")
        every { topicRepository.findById("ghost") } returns null

        val result = service.setPrerequisites("topic-b", listOf("ghost"))

        assertTrue(result.isLeft())
        result.onLeft { assertTrue(it is NotFound.ResourceNotFound) }
    }

    // -------------------------------------------------------------------------
    // setPrerequisites — cycle detection
    // -------------------------------------------------------------------------

    @Test
    fun `setPrerequisites returns 400 when setting creates a direct cycle A requires B and B requires A`() {
        // Existing graph: B -> A  (B requires A)
        // Proposed change: A -> B  would create A <-> B cycle
        every { topicRepository.findById("topic-a") } returns aTopic("topic-a")
        every { topicRepository.findById("topic-b") } returns aTopic("topic-b")
        every { prerequisiteRepository.findAllEdges() } returns listOf(Pair("topic-b", "topic-a"))

        val result = service.setPrerequisites("topic-a", listOf("topic-b"))

        assertTrue(result.isLeft())
        result.onLeft {
            assertTrue(it is BadRequest.InvalidField)
            assertTrue((it as BadRequest.InvalidField).reason.contains("cycle"))
        }
    }

    @Test
    fun `setPrerequisites returns 400 when setting creates a transitive cycle A-B-C-A`() {
        // Existing graph: B -> A, C -> B
        // Proposed change: A -> C would create A -> C -> B -> A cycle
        every { topicRepository.findById("topic-a") } returns aTopic("topic-a")
        every { topicRepository.findById("topic-c") } returns aTopic("topic-c")
        every { prerequisiteRepository.findAllEdges() } returns listOf(
            Pair("topic-b", "topic-a"),
            Pair("topic-c", "topic-b")
        )

        val result = service.setPrerequisites("topic-a", listOf("topic-c"))

        assertTrue(result.isLeft())
        result.onLeft { assertTrue(it is BadRequest.InvalidField) }
    }

    @Test
    fun `setPrerequisites saves and returns resolved topics when no cycle`() {
        // Existing graph: B -> A (B requires A)
        // Proposed: set C -> A  — no cycle
        every { topicRepository.findById("topic-c") } returns aTopic("topic-c")
        every { topicRepository.findById("topic-a") } returns aTopic("topic-a")
        every { prerequisiteRepository.findAllEdges() } returns listOf(Pair("topic-b", "topic-a"))
        every { prerequisiteRepository.setPrerequisites("topic-c", listOf("topic-a")) } returns Unit

        val result = service.setPrerequisites("topic-c", listOf("topic-a"))

        assertTrue(result.isRight())
        val topics = result.getOrNull()!!
        assertEquals(1, topics.size)
        assertEquals("topic-a", topics[0].id)
        verify { prerequisiteRepository.setPrerequisites("topic-c", listOf("topic-a")) }
    }

    @Test
    fun `setPrerequisites clears prerequisites when empty list is provided`() {
        every { topicRepository.findById("topic-b") } returns aTopic("topic-b")
        every { prerequisiteRepository.findAllEdges() } returns listOf(Pair("topic-b", "topic-a"))
        every { prerequisiteRepository.setPrerequisites("topic-b", emptyList()) } returns Unit

        val result = service.setPrerequisites("topic-b", emptyList())

        assertTrue(result.isRight())
        assertEquals(emptyList<Topic>(), result.getOrNull())
        verify { prerequisiteRepository.setPrerequisites("topic-b", emptyList()) }
    }

    // -------------------------------------------------------------------------
    // getLearningGraph
    // -------------------------------------------------------------------------

    @Test
    fun `getLearningGraph returns all edges from repository`() {
        every { prerequisiteRepository.findAllEdges() } returns listOf(
            Pair("topic-b", "topic-a"),
            Pair("topic-c", "topic-b")
        )

        val graph = service.getLearningGraph()

        assertEquals(2, graph.edges.size)
        assertTrue(graph.edges.any { it.topicId == "topic-b" && it.prerequisiteTopicId == "topic-a" })
        assertTrue(graph.edges.any { it.topicId == "topic-c" && it.prerequisiteTopicId == "topic-b" })
    }

    @Test
    fun `getLearningGraph returns empty list when no edges exist`() {
        every { prerequisiteRepository.findAllEdges() } returns emptyList()

        val graph = service.getLearningGraph()

        assertTrue(graph.edges.isEmpty())
    }
}
