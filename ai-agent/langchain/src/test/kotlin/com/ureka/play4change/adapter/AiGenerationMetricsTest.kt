package com.ureka.play4change.adapter

import com.ureka.play4change.dedup.PgVectorDeduplicationService
import com.ureka.play4change.dedup.SimilarityMatch
import com.ureka.play4change.domain.AudienceLevel
import com.ureka.play4change.model.GenerationRequest
import com.ureka.play4change.model.ReuseStrategy
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.jdbc.core.JdbcTemplate

class AiGenerationMetricsTest {

    private val FAKE_TASK_JSON = """
        [
          {
            "title": "Test Task",
            "description": "Describe something",
            "hint": "Think carefully",
            "pointsReward": 10,
            "options": ["A", "B", "C", "D"],
            "correctAnswerIndex": 0
          }
        ]
    """.trimIndent()

    @Test
    fun `generateTasks records ai_generation_duration timer with GENERATION phase tag`() = runTest {
        val meterRegistry = SimpleMeterRegistry()

        val chatModel = mockk<ChatLanguageModel>()
        val embeddingModel = mockk<EmbeddingModel>()
        val deduplicationService = mockk<PgVectorDeduplicationService>()
        val jdbc = mockk<JdbcTemplate>()

        // Mock chat model to return valid task JSON
        val chatResponse = mockk<dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage>>()
        val aiMessage = mockk<dev.langchain4j.data.message.AiMessage>()
        every { aiMessage.text() } returns FAKE_TASK_JSON
        every { chatResponse.content() } returns aiMessage
        every { chatResponse.tokenUsage() } returns null
        every { chatModel.generate(any(), any<dev.langchain4j.data.message.UserMessage>()) } returns chatResponse

        // Mock embedding model to return a 1024-dim vector
        val fakeEmbedding = FloatArray(1024) { 0.1f }
        val embeddingResponse = mockk<Response<Embedding>>()
        val embedding = mockk<Embedding>()
        every { embedding.vector() } returns fakeEmbedding
        every { embeddingResponse.content() } returns embedding
        every { embeddingModel.embed(any<String>()) } returns embeddingResponse

        // Mock deduplication — not a duplicate
        every { deduplicationService.isDuplicate(any(), any(), any()) } returns false

        val adapter = LangChain4jTaskGenerationAdapter(
            chatModel = chatModel,
            embeddingModel = embeddingModel,
            deduplicationService = deduplicationService,
            jdbc = jdbc,
            meterRegistry = meterRegistry,
            defaultSubtaskCount = 3
        )

        val request = GenerationRequest(
            topicId = "topic-test-1",
            moduleId = "module-test-1",
            subjectDomain = "sustainability",
            audienceLevel = AudienceLevel.BEGINNER,
            language = "en",
            taskCount = 1,
            moduleObjective = "Learn sustainability basics"
        )

        val result = adapter.generateTasks(request)

        assertTrue(result.isRight()) { "Expected successful generation but got: $result" }

        // Verify the timer was recorded
        val timer = meterRegistry.find("ai.generation.duration")
            .tag("generation_phase", "GENERATION")
            .timer()

        assertNotNull(timer) { "Expected ai.generation.duration timer with generation_phase=GENERATION" }
        assertTrue(timer!!.count() > 0) { "Expected at least one timer observation" }

        val durationSeconds = timer.mean(java.util.concurrent.TimeUnit.SECONDS)
        assertTrue(durationSeconds >= 0.0 && durationSeconds < 120.0) {
            "Timer value $durationSeconds seconds is outside plausible range [0, 120)"
        }
    }

    @Test
    fun `generateTasks timer is recorded with generation_phase tag`() = runTest {
        val meterRegistry = SimpleMeterRegistry()

        val chatModel = mockk<ChatLanguageModel>()
        val embeddingModel = mockk<EmbeddingModel>()
        val deduplicationService = mockk<PgVectorDeduplicationService>()
        val jdbc = mockk<JdbcTemplate>()

        val chatResponse = mockk<dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage>>()
        val aiMessage = mockk<dev.langchain4j.data.message.AiMessage>()
        every { aiMessage.text() } returns FAKE_TASK_JSON
        every { chatResponse.content() } returns aiMessage
        every { chatResponse.tokenUsage() } returns null
        every { chatModel.generate(any(), any<dev.langchain4j.data.message.UserMessage>()) } returns chatResponse

        val fakeEmbedding = FloatArray(1024) { 0.1f }
        val embeddingResponse = mockk<Response<Embedding>>()
        val embedding = mockk<Embedding>()
        every { embedding.vector() } returns fakeEmbedding
        every { embeddingResponse.content() } returns embedding
        every { embeddingModel.embed(any<String>()) } returns embeddingResponse
        every { deduplicationService.isDuplicate(any(), any(), any()) } returns false

        val adapter = LangChain4jTaskGenerationAdapter(
            chatModel = chatModel,
            embeddingModel = embeddingModel,
            deduplicationService = deduplicationService,
            jdbc = jdbc,
            meterRegistry = meterRegistry,
            defaultSubtaskCount = 3
        )

        val request = GenerationRequest(
            topicId = "topic-abc-123",
            moduleId = "module-1",
            subjectDomain = "test",
            audienceLevel = AudienceLevel.BEGINNER,
            language = "en",
            taskCount = 1,
            moduleObjective = "Test objective"
        )

        adapter.generateTasks(request)

        val timer = meterRegistry.find("ai.generation.duration")
            .tag("generation_phase", "GENERATION")
            .timer()

        assertNotNull(timer) { "Expected timer with generation_phase=GENERATION" }
    }
}
