package com.ureka.play4change.application.report

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ureka.play4change.application.port.CorrectTaskCommand
import com.ureka.play4change.application.port.ReportTaskCommand
import com.ureka.play4change.application.port.TaskReportUseCase
import com.ureka.play4change.application.topic.BatchInstanceGenerationService
import com.ureka.play4change.domain.report.TaskReport
import com.ureka.play4change.domain.report.TaskReportRepository
import com.ureka.play4change.domain.report.TaskReportStatus
import com.ureka.play4change.domain.topic.PageResult
import com.ureka.play4change.domain.topic.TaskInstanceRepository
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.Conflict
import com.ureka.play4change.error.client.NotFound
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class TaskReportService(
    private val taskReportRepository: TaskReportRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val taskInstanceRepository: TaskInstanceRepository,
    private val batchInstanceGenerationService: BatchInstanceGenerationService
) : TaskReportUseCase {

    private val log = LoggerFactory.getLogger(TaskReportService::class.java)

    override fun reportTask(command: ReportTaskCommand): Either<AppError, TaskReport> = either {
        ensureNotNull(taskTemplateRepository.findById(command.taskTemplateId)) {
            NotFound.ResourceNotFound("TaskTemplate", command.taskTemplateId)
        }

        val existing = taskReportRepository.findByUserIdAndTaskTemplateId(command.userId, command.taskTemplateId)
        ensure(existing == null) {
            Conflict.DuplicateResource("TaskReport", "userId+taskTemplateId")
        }

        val report = TaskReport(
            id = UUID.randomUUID().toString(),
            taskTemplateId = command.taskTemplateId,
            userId = command.userId,
            reason = command.reason.take(500),
            status = TaskReportStatus.PENDING,
            reportedAt = OffsetDateTime.now(),
            resolvedAt = null
        )
        taskReportRepository.save(report)
    }

    override fun listByStatus(status: String, page: Int, size: Int): PageResult<TaskReport> {
        val reportStatus = runCatching { TaskReportStatus.valueOf(status.uppercase()) }
            .getOrDefault(TaskReportStatus.PENDING)
        return taskReportRepository.findByStatus(reportStatus, page, size).mapContent { it.sanitised() }
    }

    override fun getById(reportId: String): Either<AppError, TaskReport> = either {
        ensureNotNull(taskReportRepository.findById(reportId)) {
            NotFound.ResourceNotFound("TaskReport", reportId)
        }.sanitised()
    }

    override fun correct(command: CorrectTaskCommand): Either<AppError, TaskReport> = either {
        val report = ensureNotNull(taskReportRepository.findById(command.reportId)) {
            NotFound.ResourceNotFound("TaskReport", command.reportId)
        }
        ensure(report.status == TaskReportStatus.PENDING) {
            Conflict.ConcurrentModification
        }

        val template = ensureNotNull(taskTemplateRepository.findById(report.taskTemplateId)) {
            NotFound.ResourceNotFound("TaskTemplate", report.taskTemplateId)
        }

        val correctedTemplate = template.copy(
            title = command.correctedTitle,
            options = command.correctedOptions,
            correctAnswer = command.correctAnswerIndex
        )
        taskTemplateRepository.save(correctedTemplate)

        // Delete existing instances and regenerate
        taskInstanceRepository.deleteByTaskTemplateId(template.id)
        batchInstanceGenerationService.generateAndSave(listOf(correctedTemplate))

        log.info("Task report {} resolved: template {} corrected, instances regenerated", report.id, template.id)

        val resolved = report.copy(
            status = TaskReportStatus.RESOLVED,
            resolvedAt = OffsetDateTime.now()
        )
        taskReportRepository.save(resolved)
    }

    override fun dismiss(reportId: String): Either<AppError, TaskReport> = either {
        val report = ensureNotNull(taskReportRepository.findById(reportId)) {
            NotFound.ResourceNotFound("TaskReport", reportId)
        }
        ensure(report.status == TaskReportStatus.PENDING) {
            Conflict.ConcurrentModification
        }
        val dismissed = report.copy(
            status = TaskReportStatus.DISMISSED,
            resolvedAt = OffsetDateTime.now()
        )
        taskReportRepository.save(dismissed)
    }

    /** Sanitise the stored reason to prevent XSS when rendering report content (OWASP A03). */
    private fun TaskReport.sanitised() = copy(reason = Jsoup.clean(reason, Safelist.none()))

    private fun <T> PageResult<T>.mapContent(transform: (T) -> T) = copy(content = content.map(transform))
}
