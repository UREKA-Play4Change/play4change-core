package com.ureka.play4change.web.report

import arrow.core.Either
import com.ninjasquad.springmockk.MockkBean
import com.ureka.play4change.application.port.CorrectTaskCommand
import com.ureka.play4change.application.port.ReportTaskCommand
import com.ureka.play4change.application.port.TaskReportUseCase
import com.ureka.play4change.auth.application.TokenService
import com.ureka.play4change.domain.report.TaskReport
import com.ureka.play4change.domain.report.TaskReportStatus
import com.ureka.play4change.domain.topic.PageResult
import com.ureka.play4change.error.client.NotFound
import com.ureka.play4change.infra.config.SecurityConfig
import com.ureka.play4change.web.admin.AdminTaskReportController
import com.ureka.play4change.web.user.TaskReportController
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

@WebMvcTest(controllers = [TaskReportController::class, AdminTaskReportController::class])
@Import(SecurityConfig::class)
class TaskReportControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var taskReportUseCase: TaskReportUseCase

    @MockkBean
    private lateinit var tokenService: TokenService

    @MockkBean
    private lateinit var meterRegistry: MeterRegistry

    private fun userAuth(userId: String = "user-1") = authentication(
        UsernamePasswordAuthenticationToken(userId, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
    )

    private fun adminAuth(adminId: String = "admin-1") = authentication(
        UsernamePasswordAuthenticationToken(adminId, null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
    )

    private val pendingReport = TaskReport(
        id = "report-1",
        taskTemplateId = "template-1",
        userId = "user-1",
        reason = "The answer is clearly wrong",
        status = TaskReportStatus.PENDING,
        reportedAt = OffsetDateTime.now(ZoneOffset.UTC),
        resolvedAt = null
    )

    // ── POST /tasks/{id}/report ───────────────────────────────────────────────

    @Test
    fun `POST tasks id report with valid JWT and body returns 201`() {
        every {
            taskReportUseCase.reportTask(ReportTaskCommand("user-1", "template-1", "The answer is clearly wrong"))
        } returns Either.Right(pendingReport)

        mockMvc.perform(
            post("/tasks/template-1/report")
                .with(userAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"reason":"The answer is clearly wrong"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.reportId").value("report-1"))
    }

    @Test
    fun `POST tasks id report without JWT returns 401`() {
        mockMvc.perform(
            post("/tasks/template-1/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"reason":"Bad question"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    // ── GET /admin/task-reports ───────────────────────────────────────────────

    @Test
    fun `GET admin task-reports with ADMIN JWT returns 200 with page`() {
        val page = PageResult(
            content = listOf(pendingReport),
            page = 0,
            size = 20,
            totalElements = 1L,
            totalPages = 1
        )
        every { taskReportUseCase.listByStatus("PENDING", 0, 20) } returns page

        mockMvc.perform(get("/admin/task-reports?status=PENDING").with(adminAuth()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].reportId").value("report-1"))
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun `GET admin task-reports with USER JWT returns 403`() {
        mockMvc.perform(get("/admin/task-reports").with(userAuth()))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET admin task-reports without JWT returns 401`() {
        mockMvc.perform(get("/admin/task-reports"))
            .andExpect(status().isUnauthorized)
    }

    // ── POST /admin/task-reports/{id}/correct ────────────────────────────────

    @Test
    fun `POST admin task-reports id correct with ADMIN JWT returns 200`() {
        val resolved = pendingReport.copy(status = TaskReportStatus.RESOLVED, resolvedAt = OffsetDateTime.now(ZoneOffset.UTC))
        every {
            taskReportUseCase.correct(
                CorrectTaskCommand("report-1", "Fixed title", listOf("X", "Y", "Z"), 1)
            )
        } returns Either.Right(resolved)

        mockMvc.perform(
            post("/admin/task-reports/report-1/correct")
                .with(adminAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"correctedTitle":"Fixed title","correctedOptions":["X","Y","Z"],"correctAnswerIndex":1}"""
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("RESOLVED"))
    }

    @Test
    fun `POST admin task-reports id correct returns 404 for unknown report`() {
        every {
            taskReportUseCase.correct(any())
        } returns Either.Left(NotFound.ResourceNotFound("TaskReport", "no-such"))

        mockMvc.perform(
            post("/admin/task-reports/no-such/correct")
                .with(adminAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"correctedTitle":"T","correctedOptions":["A","B"],"correctAnswerIndex":0}"""
                )
        )
            .andExpect(status().isNotFound)
    }

    // ── POST /admin/task-reports/{id}/dismiss ────────────────────────────────

    @Test
    fun `POST admin task-reports id dismiss with ADMIN JWT returns 200`() {
        val dismissed = pendingReport.copy(status = TaskReportStatus.DISMISSED, resolvedAt = OffsetDateTime.now(ZoneOffset.UTC))
        every { taskReportUseCase.dismiss("report-1") } returns Either.Right(dismissed)

        mockMvc.perform(
            post("/admin/task-reports/report-1/dismiss")
                .with(adminAuth())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("DISMISSED"))
    }
}
