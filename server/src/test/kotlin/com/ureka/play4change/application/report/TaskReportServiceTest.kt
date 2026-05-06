package com.ureka.play4change.application.report

import com.ureka.play4change.application.port.CorrectTaskCommand
import com.ureka.play4change.application.port.ReportTaskCommand
import com.ureka.play4change.application.topic.BatchInstanceGenerationService
import com.ureka.play4change.domain.report.TaskReport
import com.ureka.play4change.domain.report.TaskReportRepository
import com.ureka.play4change.domain.report.TaskReportStatus
import com.ureka.play4change.domain.topic.TaskInstanceRepository
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TaskType
import com.ureka.play4change.error.client.Conflict
import com.ureka.play4change.error.client.NotFound
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class TaskReportServiceTest {

    private val taskReportRepository = mockk<TaskReportRepository>()
    private val taskTemplateRepository = mockk<TaskTemplateRepository>()
    private val taskInstanceRepository = mockk<TaskInstanceRepository>()
    private val batchInstanceGenerationService = mockk<BatchInstanceGenerationService>(relaxed = true)

    private val service = TaskReportService(
        taskReportRepository,
        taskTemplateRepository,
        taskInstanceRepository,
        batchInstanceGenerationService
    )

    private val userId = "user-1"
    private val templateId = "template-1"

    private val template = TaskTemplate(
        id = templateId,
        moduleId = "module-1",
        dayIndex = 0,
        poolIndex = 0,
        title = "Original title",
        description = "Description",
        hint = null,
        taskType = TaskType.MULTIPLE_CHOICE,
        pointsReward = 20,
        options = listOf("A", "B", "C"),
        correctAnswer = 0,
        version = 1,
        isCurrent = true,
        supersededBy = null,
        embedding = null,
        language = "en",
        createdAt = OffsetDateTime.now(ZoneOffset.UTC)
    )

    private fun makeReport(
        id: String = "report-1",
        status: TaskReportStatus = TaskReportStatus.PENDING
    ) = TaskReport(
        id = id,
        taskTemplateId = templateId,
        userId = userId,
        reason = "The answer is wrong",
        status = status,
        reportedAt = OffsetDateTime.now(ZoneOffset.UTC),
        resolvedAt = null
    )

    @Test
    fun `reporting a valid task creates a PENDING report`() {
        every { taskTemplateRepository.findById(templateId) } returns template
        every { taskReportRepository.findByUserIdAndTaskTemplateId(userId, templateId) } returns null
        every { taskReportRepository.save(any()) } answers { firstArg() }

        val result = service.reportTask(ReportTaskCommand(userId, templateId, "The answer is wrong"))

        val saved = result.getOrNull()!!
        assertEquals(TaskReportStatus.PENDING, saved.status)
        assertEquals(templateId, saved.taskTemplateId)
        assertEquals(userId, saved.userId)
    }

    @Test
    fun `second report from same user on same task returns conflict`() {
        every { taskTemplateRepository.findById(templateId) } returns template
        every { taskReportRepository.findByUserIdAndTaskTemplateId(userId, templateId) } returns makeReport()

        val result = service.reportTask(ReportTaskCommand(userId, templateId, "Again"))

        val error = result.leftOrNull()
        assert(error is Conflict.DuplicateResource)
    }

    @Test
    fun `correcting a report triggers instance regeneration and marks report RESOLVED`() {
        val report = makeReport()
        every { taskReportRepository.findById("report-1") } returns report
        every { taskTemplateRepository.findById(templateId) } returns template
        every { taskTemplateRepository.save(any()) } answers { firstArg() }
        every { taskInstanceRepository.deleteByTaskTemplateId(templateId) } returns Unit
        every { taskReportRepository.save(any()) } answers { firstArg() }

        val result = service.correct(
            CorrectTaskCommand(
                reportId = "report-1",
                correctedTitle = "Fixed title",
                correctedOptions = listOf("X", "Y", "Z"),
                correctAnswerIndex = 1
            )
        )

        assertEquals(TaskReportStatus.RESOLVED, result.getOrNull()!!.status)
        verify { taskInstanceRepository.deleteByTaskTemplateId(templateId) }
        verify { batchInstanceGenerationService.generateAndSave(any()) }
    }

    @Test
    fun `dismissing a report marks it DISMISSED without changing the task`() {
        val report = makeReport()
        every { taskReportRepository.findById("report-1") } returns report
        every { taskReportRepository.save(any()) } answers { firstArg() }

        val result = service.dismiss("report-1")

        val savedSlot = slot<TaskReport>()
        verify { taskReportRepository.save(capture(savedSlot)) }
        assertEquals(TaskReportStatus.DISMISSED, savedSlot.captured.status)
        verify(exactly = 0) { taskTemplateRepository.save(any()) }
        verify(exactly = 0) { taskInstanceRepository.deleteByTaskTemplateId(any()) }
        assertEquals(TaskReportStatus.DISMISSED, result.getOrNull()!!.status)
    }

    @Test
    fun `reporting a non-existent task returns not found`() {
        every { taskTemplateRepository.findById("unknown") } returns null

        val result = service.reportTask(ReportTaskCommand(userId, "unknown", "bad question"))

        val error = result.leftOrNull()
        assert(error is NotFound.ResourceNotFound)
    }
}
