package com.ureka.play4change.web.topic

import arrow.core.right
import com.ninjasquad.springmockk.MockkBean
import com.ureka.play4change.application.port.TopicDetail
import com.ureka.play4change.application.port.TopicUseCase
import com.ureka.play4change.auth.adapter.inbound.security.RateLimitService
import com.ureka.play4change.auth.application.TokenService
import com.ureka.play4change.domain.topic.AudienceLevel
import com.ureka.play4change.domain.topic.ContentSourceType
import com.ureka.play4change.domain.topic.GenerationPhase
import com.ureka.play4change.domain.topic.Topic
import com.ureka.play4change.domain.topic.TopicPhaseLog
import com.ureka.play4change.domain.topic.TopicStatus
import com.ureka.play4change.infra.config.SecurityConfig
import com.ureka.play4change.web.admin.SseTopicEventPublisher
import com.ureka.play4change.web.admin.TopicController
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime

@WebMvcTest(controllers = [TopicController::class])
@Import(SecurityConfig::class)
class TopicControllerPhaseTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var topicUseCase: TopicUseCase

    @MockkBean
    private lateinit var tokenService: TokenService

    @MockkBean
    private lateinit var rateLimitService: RateLimitService

    @MockkBean
    private lateinit var meterRegistry: MeterRegistry

    @MockkBean
    private lateinit var ssePublisher: SseTopicEventPublisher

    private fun adminAuth() = authentication(
        UsernamePasswordAuthenticationToken("admin-1", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
    )

    private val now = OffsetDateTime.now()

    private fun makeTopic(phase: GenerationPhase = GenerationPhase.INDEXING): Topic = Topic(
        id = "topic-1",
        title = "Java Basics",
        description = "Core Java",
        category = "programming",
        contentSourceType = ContentSourceType.URL,
        contentSourceRef = "https://example.com",
        rawExtractedText = null,
        taskCount = 5,
        subscriptionWindowDays = 7,
        expiresAt = now.plusDays(30),
        audienceLevel = AudienceLevel.BEGINNER,
        language = "en",
        status = TopicStatus.GENERATING,
        createdBy = "admin-1",
        createdAt = now,
        currentPhase = phase,
        phaseUpdatedAt = now
    )

    private fun makeLog(): List<TopicPhaseLog> = listOf(
        TopicPhaseLog(
            id = "log-1",
            topicId = "topic-1",
            fromPhase = GenerationPhase.INGESTION,
            toPhase = GenerationPhase.ANALYSIS,
            transitionedAt = now.minusSeconds(30),
            durationMs = 1500
        ),
        TopicPhaseLog(
            id = "log-2",
            topicId = "topic-1",
            fromPhase = GenerationPhase.ANALYSIS,
            toPhase = GenerationPhase.GENERATION,
            transitionedAt = now.minusSeconds(20),
            durationMs = 10000
        ),
        TopicPhaseLog(
            id = "log-3",
            topicId = "topic-1",
            fromPhase = GenerationPhase.GENERATION,
            toPhase = GenerationPhase.INDEXING,
            transitionedAt = now.minusSeconds(5),
            durationMs = 15000
        )
    )

    @Test
    fun `GET admin topics id returns currentPhase field`() {
        every { topicUseCase.getByIdWithLog("topic-1") } returns
            TopicDetail(makeTopic(GenerationPhase.INDEXING), emptyList()).right()

        mockMvc.perform(get("/admin/topics/topic-1").with(adminAuth()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentPhase").value("INDEXING"))
            .andExpect(jsonPath("$.phaseUpdatedAt").isNotEmpty)
    }

    @Test
    fun `generationLog contains timestamps and durations`() {
        every { topicUseCase.getByIdWithLog("topic-1") } returns
            TopicDetail(makeTopic(), makeLog()).right()

        mockMvc.perform(get("/admin/topics/topic-1").with(adminAuth()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.generationLog").isArray)
            .andExpect(jsonPath("$.generationLog.length()").value(3))
            .andExpect(jsonPath("$.generationLog[0].fromPhase").value("INGESTION"))
            .andExpect(jsonPath("$.generationLog[0].toPhase").value("ANALYSIS"))
            .andExpect(jsonPath("$.generationLog[0].durationMs").value(1500))
            .andExpect(jsonPath("$.generationLog[1].fromPhase").value("ANALYSIS"))
            .andExpect(jsonPath("$.generationLog[1].toPhase").value("GENERATION"))
            .andExpect(jsonPath("$.generationLog[2].fromPhase").value("GENERATION"))
            .andExpect(jsonPath("$.generationLog[2].toPhase").value("INDEXING"))
            .andExpect(jsonPath("$.generationLog[2].durationMs").value(15000))
    }
}
