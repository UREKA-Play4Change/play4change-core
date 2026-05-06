package com.ureka.play4change.web.admin.dto

import com.ureka.play4change.domain.report.TaskReport
import java.time.OffsetDateTime

data class TaskReportResponse(
    val reportId: String,
    val taskTemplateId: String,
    val userId: String,
    val reason: String,
    val status: String,
    val reportedAt: OffsetDateTime,
    val resolvedAt: OffsetDateTime?
) {
    companion object {
        fun from(report: TaskReport) = TaskReportResponse(
            reportId = report.id,
            taskTemplateId = report.taskTemplateId,
            userId = report.userId,
            reason = report.reason,
            status = report.status.name,
            reportedAt = report.reportedAt,
            resolvedAt = report.resolvedAt
        )
    }
}
