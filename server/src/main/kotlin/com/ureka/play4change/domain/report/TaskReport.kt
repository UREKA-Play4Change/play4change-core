package com.ureka.play4change.domain.report

import java.time.OffsetDateTime

data class TaskReport(
    val id: String,
    val taskTemplateId: String,
    val userId: String,
    val reason: String,
    val status: TaskReportStatus,
    val reportedAt: OffsetDateTime,
    val resolvedAt: OffsetDateTime?
)
