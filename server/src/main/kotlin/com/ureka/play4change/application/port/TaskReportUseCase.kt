package com.ureka.play4change.application.port

import arrow.core.Either
import com.ureka.play4change.domain.report.TaskReport
import com.ureka.play4change.domain.topic.PageResult
import com.ureka.play4change.error.AppError

data class ReportTaskCommand(
    val userId: String,
    val assignmentId: String,
    val reason: String
)

data class CorrectTaskCommand(
    val reportId: String,
    val correctedTitle: String,
    val correctedOptions: List<String>,
    val correctAnswerIndex: Int
)

interface TaskReportUseCase {
    fun reportTask(command: ReportTaskCommand): Either<AppError, TaskReport>
    fun listByStatus(status: String, page: Int, size: Int): PageResult<TaskReport>
    fun getById(reportId: String): Either<AppError, TaskReport>
    fun correct(command: CorrectTaskCommand): Either<AppError, TaskReport>
    fun dismiss(reportId: String): Either<AppError, TaskReport>
}
