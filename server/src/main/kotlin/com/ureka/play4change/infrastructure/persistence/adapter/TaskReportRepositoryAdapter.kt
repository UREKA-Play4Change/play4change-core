package com.ureka.play4change.infrastructure.persistence.adapter

import com.ureka.play4change.domain.report.TaskReport
import com.ureka.play4change.domain.report.TaskReportRepository
import com.ureka.play4change.domain.report.TaskReportStatus
import com.ureka.play4change.domain.topic.PageResult
import com.ureka.play4change.infrastructure.persistence.entity.TaskReportEntity
import com.ureka.play4change.infrastructure.persistence.repository.TaskReportJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.TaskTemplateJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class TaskReportRepositoryAdapter(
    private val jpa: TaskReportJpaRepository,
    private val templateJpa: TaskTemplateJpaRepository
) : TaskReportRepository {

    override fun save(report: TaskReport): TaskReport =
        jpa.save(report.toEntity()).toDomain()

    override fun findById(id: String): TaskReport? =
        jpa.findById(id).orElse(null)?.toDomain()

    override fun findByUserIdAndTaskTemplateId(userId: String, taskTemplateId: String): TaskReport? =
        jpa.findByUserIdAndTaskTemplateId(userId, taskTemplateId)?.toDomain()

    override fun findByStatus(status: TaskReportStatus, page: Int, size: Int): PageResult<TaskReport> {
        val pageResult = jpa.findByStatus(status.name, PageRequest.of(page, size))
        return PageResult(
            content = pageResult.content.map { it.toDomain() },
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages
        )
    }

    private fun TaskReport.toEntity(): TaskReportEntity {
        val templateRef = templateJpa.getReferenceById(taskTemplateId)
        return TaskReportEntity(
            id = id,
            taskTemplate = templateRef,
            userId = userId,
            reason = reason,
            status = status.name,
            reportedAt = reportedAt,
            resolvedAt = resolvedAt
        )
    }

    private fun TaskReportEntity.toDomain() = TaskReport(
        id = id,
        taskTemplateId = taskTemplate.id,
        userId = userId,
        reason = reason,
        status = TaskReportStatus.valueOf(status),
        reportedAt = reportedAt,
        resolvedAt = resolvedAt
    )
}
