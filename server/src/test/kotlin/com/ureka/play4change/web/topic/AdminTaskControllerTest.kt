package com.ureka.play4change.web.topic

import arrow.core.left
import arrow.core.right
import com.ninjasquad.springmockk.MockkBean
import com.ureka.play4change.application.port.AdminTaskUseCase
import com.ureka.play4change.application.port.TaskTemplateWithStats
import com.ureka.play4change.application.port.UpdateTaskCommand
import com.ureka.play4change.auth.adapter.inbound.security.RateLimitService
import com.ureka.play4change.auth.application.TokenService
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.infrastructure.persistence.repository.ExplanationMessageJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.ExplanationSessionJpaRepository
import com.ureka.play4change.domain.struggle.AdaptiveTask
import com.ureka.play4change.domain.struggle.AdaptiveTaskAdminView
import com.ureka.play4change.domain.struggle.ErrorPattern
import com.ureka.play4change.domain.struggle.StruggleStatus
import com.ureka.play4change.domain.topic.TaskQuestionStats
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskType
import com.ureka.play4change.error.client.NotFound
import com.ureka.play4change.infra.config.SecurityConfig
import com.ureka.play4change.web.admin.AdminTaskController
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime

@WebMvcTest(controllers = [AdminTaskController::class])
@Import(SecurityConfig::class)
class AdminTaskControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var adminTaskUseCase: AdminTaskUseCase

    @MockkBean
    private lateinit var tokenService: TokenService

    @MockkBean
    private lateinit var rateLimitService: RateLimitService

    @MockkBean
    private lateinit var meterRegistry: MeterRegistry

    @MockkBean
    private lateinit var explanationSessionJpa: ExplanationSessionJpaRepository

    @MockkBean
    private lateinit var explanationMessageJpa: ExplanationMessageJpaRepository

    @MockkBean
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setup() {
        every { rateLimitService.tryConsume(any(), any()) } returns true
    }

    private fun adminAuth() = authentication(
        UsernamePasswordAuthenticationToken("admin-1", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
    )

    private fun aTemplate() = TaskTemplate(
        id = "template-1",
        moduleId = "module-1",
        dayIndex = 0,
        poolIndex = 0,
        title = "What is SDG?",
        description = "Choose the definition",
        hint = null,
        taskType = TaskType.MULTIPLE_CHOICE,
        pointsReward = 20,
        options = listOf("A", "B", "C", "D"),
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
            title = "Simpler question",
            description = "Pick one",
            hint = null,
            pointsReward = 10,
            orderIndex = 0,
            completedAt = null,
            isCorrect = null,
            options = listOf("X", "Y"),
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
    fun `GET admin topics topicId tasks with valid admin JWT returns 200 with question list`() {
        every { adminTaskUseCase.getTasksForTopic("topic-1") } returns
            listOf(TaskTemplateWithStats(aTemplate(), TaskQuestionStats.ZERO)).right()

        mockMvc.perform(
            get("/admin/topics/topic-1/tasks")
                .with(adminAuth())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value("template-1"))
            .andExpect(jsonPath("$[0].title").value("What is SDG?"))
            .andExpect(jsonPath("$[0].stats.totalAttempts").value(0))
    }

    @Test
    fun `GET admin topics topicId tasks without JWT returns 401`() {
        mockMvc.perform(get("/admin/topics/topic-1/tasks"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET admin topics topicId tasks returns 404 when topic not found`() {
        every { adminTaskUseCase.getTasksForTopic("bad-id") } returns
            NotFound.ResourceNotFound("Topic", "bad-id").left()

        mockMvc.perform(
            get("/admin/topics/bad-id/tasks")
                .with(adminAuth())
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET admin topics topicId struggle-tasks returns 200 with adaptive task list`() {
        every { adminTaskUseCase.getStruggleTasksForTopic("topic-1") } returns
            listOf(anAdaptiveView()).right()

        mockMvc.perform(
            get("/admin/topics/topic-1/struggle-tasks")
                .with(adminAuth())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value("adaptive-1"))
            .andExpect(jsonPath("$[0].sessionStatus").value("OPEN"))
            .andExpect(jsonPath("$[0].errorPattern").value("WRONG_CONCEPT"))
    }

    @Test
    fun `PUT admin tasks templateId with valid body returns 200 with updated template`() {
        every {
            adminTaskUseCase.updateTask(
                "template-1",
                UpdateTaskCommand("New title", "New desc", "A hint", listOf("A", "B", "C"), 0)
            )
        } returns aTemplate().copy(title = "New title").right()

        mockMvc.perform(
            put("/admin/tasks/template-1")
                .with(adminAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"title":"New title","description":"New desc","hint":"A hint",""" +
                        """"options":["A","B","C"],"correctAnswer":0}"""
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("template-1"))
    }

    @Test
    fun `PUT admin tasks templateId without JWT returns 401`() {
        mockMvc.perform(
            put("/admin/tasks/template-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"T","description":"D","hint":null,"options":null,"correctAnswer":null}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `PUT admin tasks templateId with blank title returns 400`() {
        mockMvc.perform(
            put("/admin/tasks/template-1")
                .with(adminAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"","description":"D","hint":null,"options":null,"correctAnswer":null}""")
        )
            .andExpect(status().isBadRequest)
    }
}
