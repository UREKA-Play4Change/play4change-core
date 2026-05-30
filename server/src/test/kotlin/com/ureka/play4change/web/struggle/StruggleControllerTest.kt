package com.ureka.play4change.web.struggle

import arrow.core.Either
import com.ninjasquad.springmockk.MockkBean
import com.ureka.play4change.application.port.AdaptiveSubmitResult
import com.ureka.play4change.application.port.StruggleUseCase
import com.ureka.play4change.application.port.SubmitAdaptiveTaskCommand
import com.ureka.play4change.auth.adapter.inbound.security.RateLimitService
import com.ureka.play4change.auth.application.TokenService
import com.ureka.play4change.domain.struggle.AdaptiveTask
import com.ureka.play4change.domain.struggle.ErrorPattern
import com.ureka.play4change.domain.struggle.StruggleSession
import com.ureka.play4change.domain.struggle.StruggleStatus
import com.ureka.play4change.error.client.NotFound
import com.ureka.play4change.infra.config.SecurityConfig
import com.ureka.play4change.web.user.StruggleController
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
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
import java.time.ZoneOffset

@WebMvcTest(controllers = [StruggleController::class])
@Import(SecurityConfig::class)
class StruggleControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var struggleUseCase: StruggleUseCase

    @MockkBean
    private lateinit var tokenService: TokenService

    @MockkBean
    private lateinit var rateLimitService: RateLimitService

    @MockkBean
    private lateinit var meterRegistry: MeterRegistry

    private fun userAuth(userId: String = "user-1") = authentication(
        UsernamePasswordAuthenticationToken(userId, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
    )

    private fun makeAdaptiveTask(id: String, orderIndex: Int) = AdaptiveTask(
        id = id,
        struggleSessionId = "session-1",
        title = "Adaptive task $orderIndex",
        description = "Description",
        hint = null,
        pointsReward = 10,
        orderIndex = orderIndex,
        completedAt = null,
        isCorrect = null,
        options = listOf("A", "B", "C"),
        correctAnswer = 0,
        selectedOption = null,
        optionOrder = listOf(0, 1, 2)
    )

    private val activeSession = StruggleSession(
        id = "session-1",
        enrollmentId = "enrollment-1",
        originalTaskAssignmentId = "assignment-1",
        errorPattern = ErrorPattern.WRONG_CONCEPT,
        attemptCount = 2,
        detectedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5),
        resolvedAt = null,
        status = StruggleStatus.OPEN,
        adaptiveTasks = listOf(
            makeAdaptiveTask("task-1", 0),
            makeAdaptiveTask("task-2", 1),
            makeAdaptiveTask("task-3", 2)
        )
    )

    // ── GET /struggle/enrollment/{enrollmentId} ───────────────────────────────

    @Test
    fun `GET struggle enrollment id with valid JWT returns 200 with active session`() {
        every { struggleUseCase.getSession("user-1", "enrollment-1") } returns Either.Right(activeSession)

        mockMvc.perform(get("/struggle/enrollment/enrollment-1").with(userAuth()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sessionId").value("session-1"))
            .andExpect(jsonPath("$.errorPattern").value("WRONG_CONCEPT"))
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.adaptiveTasks.length()").value(3))
    }

    @Test
    fun `GET struggle enrollment id returns 404 when no active session exists`() {
        every { struggleUseCase.getSession("user-1", "enrollment-1") } returns
            Either.Left(NotFound.ResourceNotFound("StruggleSession", "enrollment-1"))

        mockMvc.perform(get("/struggle/enrollment/enrollment-1").with(userAuth()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET struggle enrollment id without JWT returns 401`() {
        mockMvc.perform(get("/struggle/enrollment/enrollment-1"))
            .andExpect(status().isUnauthorized)
    }

    // ── POST /struggle/{sessionId}/tasks/{taskId}/submit ──────────────────────

    @Test
    fun `POST struggle session tasks taskId submit with valid JWT returns 200`() {
        val completedTask = makeAdaptiveTask("task-1", 0).copy(
            completedAt = OffsetDateTime.now(ZoneOffset.UTC),
            isCorrect = true,
            selectedOption = 0
        )
        val submitResult = AdaptiveSubmitResult(
            task = completedTask,
            isCorrect = true,
            pointsAwarded = 10,
            sessionResolved = false
        )
        every {
            struggleUseCase.submitAdaptiveTask(
                SubmitAdaptiveTaskCommand("user-1", "session-1", "task-1", 0)
            )
        } returns Either.Right(submitResult)

        mockMvc.perform(
            post("/struggle/session-1/tasks/task-1/submit")
                .with(userAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"selectedOption":0}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.taskId").value("task-1"))
            .andExpect(jsonPath("$.title").value("Adaptive task 0"))
    }

    @Test
    fun `POST struggle session tasks taskId submit without JWT returns 401`() {
        mockMvc.perform(
            post("/struggle/session-1/tasks/task-1/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"selectedOption":0}""")
        )
            .andExpect(status().isUnauthorized)
    }
}
