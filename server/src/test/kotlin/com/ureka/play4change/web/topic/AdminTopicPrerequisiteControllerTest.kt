package com.ureka.play4change.web.topic

import arrow.core.left
import arrow.core.right
import com.ninjasquad.springmockk.MockkBean
import com.ureka.play4change.application.port.LearningGraph
import com.ureka.play4change.application.port.PrerequisiteEdge
import com.ureka.play4change.application.port.TopicUseCase
import com.ureka.play4change.auth.adapter.inbound.security.RateLimitService
import com.ureka.play4change.auth.application.TokenService
import com.ureka.play4change.domain.topic.AudienceLevel
import com.ureka.play4change.domain.topic.ContentSourceType
import com.ureka.play4change.domain.topic.GenerationPhase
import com.ureka.play4change.domain.topic.Topic
import com.ureka.play4change.domain.topic.TopicStatus
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.error.client.NotFound
import com.ureka.play4change.infrastructure.config.SecurityConfig
import com.ureka.play4change.web.admin.AdminTopicPrerequisiteController
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime

@WebMvcTest(controllers = [AdminTopicPrerequisiteController::class])
@Import(SecurityConfig::class)
class AdminTopicPrerequisiteControllerTest {

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

    @BeforeEach
    fun setup() {
        every { rateLimitService.tryConsume(any(), any()) } returns true
    }

    private fun adminAuth() = authentication(
        UsernamePasswordAuthenticationToken("admin-1", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
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
    // GET /admin/topics/{id}/prerequisites
    // -------------------------------------------------------------------------

    @Test
    fun `GET prerequisites without JWT returns 401`() {
        mockMvc.perform(get("/admin/topics/topic-b/prerequisites"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET prerequisites returns 404 when topic not found`() {
        every { topicUseCase.getPrerequisites("missing") } returns
            NotFound.ResourceNotFound("Topic", "missing").left()

        mockMvc.perform(
            get("/admin/topics/missing/prerequisites")
                .with(adminAuth())
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET prerequisites returns 200 with empty list when no prerequisites`() {
        every { topicUseCase.getPrerequisites("topic-b") } returns emptyList<Topic>().right()

        mockMvc.perform(
            get("/admin/topics/topic-b/prerequisites")
                .with(adminAuth())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `GET prerequisites returns 200 with prerequisite topics`() {
        every { topicUseCase.getPrerequisites("topic-b") } returns listOf(aTopic("topic-a", "Alpha")).right()

        mockMvc.perform(
            get("/admin/topics/topic-b/prerequisites")
                .with(adminAuth())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value("topic-a"))
            .andExpect(jsonPath("$[0].title").value("Alpha"))
            .andExpect(jsonPath("$[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$[0].category").value("DIGITAL"))
    }

    // -------------------------------------------------------------------------
    // POST /admin/topics/{id}/prerequisites
    // -------------------------------------------------------------------------

    @Test
    fun `POST prerequisites without JWT returns 401`() {
        mockMvc.perform(
            post("/admin/topics/topic-b/prerequisites")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"prerequisiteIds":["topic-a"]}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST prerequisites returns 404 when topic not found`() {
        every { topicUseCase.setPrerequisites("missing", listOf("topic-a")) } returns
            NotFound.ResourceNotFound("Topic", "missing").left()

        mockMvc.perform(
            post("/admin/topics/missing/prerequisites")
                .with(adminAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"prerequisiteIds":["topic-a"]}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST prerequisites returns 400 when cycle would be created`() {
        every { topicUseCase.setPrerequisites("topic-a", listOf("topic-b")) } returns
            BadRequest.InvalidField("prerequisiteIds", "setting these prerequisites would create a cycle").left()

        mockMvc.perform(
            post("/admin/topics/topic-a/prerequisites")
                .with(adminAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"prerequisiteIds":["topic-b"]}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST prerequisites returns 200 with saved prerequisite topics`() {
        every { topicUseCase.setPrerequisites("topic-b", listOf("topic-a")) } returns
            listOf(aTopic("topic-a", "Alpha")).right()

        mockMvc.perform(
            post("/admin/topics/topic-b/prerequisites")
                .with(adminAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"prerequisiteIds":["topic-a"]}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value("topic-a"))
            .andExpect(jsonPath("$[0].title").value("Alpha"))
    }

    @Test
    fun `POST prerequisites with empty list clears prerequisites and returns 200`() {
        every { topicUseCase.setPrerequisites("topic-b", emptyList()) } returns emptyList<Topic>().right()

        mockMvc.perform(
            post("/admin/topics/topic-b/prerequisites")
                .with(adminAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"prerequisiteIds":[]}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    // -------------------------------------------------------------------------
    // GET /admin/learning-graph
    // -------------------------------------------------------------------------

    @Test
    fun `GET learning-graph without JWT returns 401`() {
        mockMvc.perform(get("/admin/learning-graph"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET learning-graph returns 200 with all edges`() {
        every { topicUseCase.getLearningGraph() } returns LearningGraph(
            edges = listOf(
                PrerequisiteEdge("topic-b", "topic-a"),
                PrerequisiteEdge("topic-c", "topic-b")
            )
        )

        mockMvc.perform(
            get("/admin/learning-graph")
                .with(adminAuth())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.edges.length()").value(2))
            .andExpect(jsonPath("$.edges[0].topicId").value("topic-b"))
            .andExpect(jsonPath("$.edges[0].prerequisiteTopicId").value("topic-a"))
            .andExpect(jsonPath("$.edges[1].topicId").value("topic-c"))
            .andExpect(jsonPath("$.edges[1].prerequisiteTopicId").value("topic-b"))
    }

    @Test
    fun `GET learning-graph returns 200 with empty edges list when graph is empty`() {
        every { topicUseCase.getLearningGraph() } returns LearningGraph(edges = emptyList())

        mockMvc.perform(
            get("/admin/learning-graph")
                .with(adminAuth())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.edges").isArray)
            .andExpect(jsonPath("$.edges.length()").value(0))
    }
}
