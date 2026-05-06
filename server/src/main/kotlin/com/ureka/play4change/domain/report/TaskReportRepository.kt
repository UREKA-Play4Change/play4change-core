package com.ureka.play4change.domain.report

import com.ureka.play4change.domain.topic.PageResult

interface TaskReportRepository {
    fun save(report: TaskReport): TaskReport
    fun findById(id: String): TaskReport?
    fun findByUserIdAndTaskTemplateId(userId: String, taskTemplateId: String): TaskReport?
    fun findByStatus(status: TaskReportStatus, page: Int, size: Int): PageResult<TaskReport>
}
