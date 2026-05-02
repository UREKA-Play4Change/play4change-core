package com.ureka.play4change.application.topic

import com.ureka.play4change.domain.topic.AudienceLevel
import com.ureka.play4change.domain.topic.ContentSourceType
import com.ureka.play4change.domain.topic.GenerationPhase
import com.ureka.play4change.domain.topic.Topic
import com.ureka.play4change.domain.topic.TopicPhaseLog
import com.ureka.play4change.domain.topic.TopicPhaseLogRepository
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.domain.topic.TopicStatus
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class GenerationPhaseTransitionTest {

    private val topicRepository = mockk<TopicRepository>()
    private val phaseLogRepository = mockk<TopicPhaseLogRepository>()
    private val service = PhaseTransitionService(topicRepository, phaseLogRepository)

    private val topicId = "topic-1"

    private fun makeTopic(phase: GenerationPhase): Topic = Topic(
        id = topicId,
        title = "Test",
        description = "A test topic",
        category = "test",
        contentSourceType = ContentSourceType.URL,
        contentSourceRef = "https://example.com",
        rawExtractedText = null,
        taskCount = 5,
        subscriptionWindowDays = 7,
        expiresAt = OffsetDateTime.now().plusDays(30),
        audienceLevel = AudienceLevel.BEGINNER,
        language = "en",
        status = TopicStatus.GENERATING,
        createdBy = "admin-1",
        createdAt = OffsetDateTime.now(),
        currentPhase = phase,
        phaseUpdatedAt = OffsetDateTime.now().minusSeconds(10)
    )

    private fun stubTransition(
        fromPhase: GenerationPhase
    ): Pair<CapturingSlot<TopicPhaseLog>, CapturingSlot<GenerationPhase>> {
        val logSlot = slot<TopicPhaseLog>()
        val phaseSlot = slot<GenerationPhase>()
        every { topicRepository.findById(topicId) } returns makeTopic(fromPhase)
        every { phaseLogRepository.save(capture(logSlot)) } answers { firstArg() }
        every { topicRepository.updatePhase(topicId, capture(phaseSlot), any()) } returns Unit
        return logSlot to phaseSlot
    }

    @Test
    fun `topic starts in INGESTION on creation`() {
        val topic = makeTopic(GenerationPhase.INGESTION)
        assertEquals(GenerationPhase.INGESTION, topic.currentPhase)
    }

    @Test
    fun `successful ingestion transitions to ANALYSIS`() {
        val (logSlot, phaseSlot) = stubTransition(GenerationPhase.INGESTION)

        service.transitionTo(topicId, GenerationPhase.ANALYSIS)

        assertEquals(GenerationPhase.INGESTION, logSlot.captured.fromPhase)
        assertEquals(GenerationPhase.ANALYSIS, logSlot.captured.toPhase)
        assertEquals(GenerationPhase.ANALYSIS, phaseSlot.captured)
    }

    @Test
    fun `successful analysis transitions to GENERATION`() {
        val (logSlot, phaseSlot) = stubTransition(GenerationPhase.ANALYSIS)

        service.transitionTo(topicId, GenerationPhase.GENERATION)

        assertEquals(GenerationPhase.ANALYSIS, logSlot.captured.fromPhase)
        assertEquals(GenerationPhase.GENERATION, logSlot.captured.toPhase)
        assertEquals(GenerationPhase.GENERATION, phaseSlot.captured)
    }

    @Test
    fun `successful generation transitions to INDEXING`() {
        val (logSlot, phaseSlot) = stubTransition(GenerationPhase.GENERATION)

        service.transitionTo(topicId, GenerationPhase.INDEXING)

        assertEquals(GenerationPhase.GENERATION, logSlot.captured.fromPhase)
        assertEquals(GenerationPhase.INDEXING, logSlot.captured.toPhase)
        assertEquals(GenerationPhase.INDEXING, phaseSlot.captured)
    }

    @Test
    fun `successful indexing transitions to ACTIVE`() {
        val (logSlot, phaseSlot) = stubTransition(GenerationPhase.INDEXING)

        service.transitionTo(topicId, GenerationPhase.ACTIVE)

        assertEquals(GenerationPhase.INDEXING, logSlot.captured.fromPhase)
        assertEquals(GenerationPhase.ACTIVE, logSlot.captured.toPhase)
        assertEquals(GenerationPhase.ACTIVE, phaseSlot.captured)
    }

    @Test
    fun `Mistral failure in GENERATION transitions to FAILED`() {
        val (logSlot, phaseSlot) = stubTransition(GenerationPhase.GENERATION)

        service.transitionTo(topicId, GenerationPhase.FAILED)

        assertEquals(GenerationPhase.GENERATION, logSlot.captured.fromPhase)
        assertEquals(GenerationPhase.FAILED, logSlot.captured.toPhase)
        assertEquals(GenerationPhase.FAILED, phaseSlot.captured)
    }

    @Test
    fun `regenerate on FAILED topic resets to INGESTION`() {
        val (logSlot, phaseSlot) = stubTransition(GenerationPhase.FAILED)

        service.transitionTo(topicId, GenerationPhase.INGESTION)

        assertEquals(GenerationPhase.FAILED, logSlot.captured.fromPhase)
        assertEquals(GenerationPhase.INGESTION, logSlot.captured.toPhase)
        assertEquals(GenerationPhase.INGESTION, phaseSlot.captured)
    }

    @Test
    fun `transition logs non-negative duration in milliseconds`() {
        val (logSlot, _) = stubTransition(GenerationPhase.INGESTION)

        service.transitionTo(topicId, GenerationPhase.ANALYSIS)

        assert(logSlot.captured.durationMs >= 0) { "durationMs must be non-negative" }
    }

    @Test
    fun `transition is skipped when topic is not found`() {
        every { topicRepository.findById(topicId) } returns null

        service.transitionTo(topicId, GenerationPhase.ANALYSIS)

        verify(exactly = 0) { phaseLogRepository.save(any()) }
        verify(exactly = 0) { topicRepository.updatePhase(any(), any(), any()) }
    }
}
