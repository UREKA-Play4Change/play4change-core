package com.ureka.play4change.web.admin

import com.ureka.play4change.application.port.CorrectTaskCommand
import com.ureka.play4change.application.port.TaskReportUseCase
import com.ureka.play4change.error.AppError
import com.ureka.play4change.web.admin.dto.CorrectTaskReportRequest
import com.ureka.play4change.web.admin.dto.PageResponse
import com.ureka.play4change.web.admin.dto.TaskReportResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/task-reports")
class AdminTaskReportController(private val taskReportUseCase: TaskReportUseCase) {

    @GetMapping
    fun listReports(
        @RequestParam(defaultValue = "PENDING") status: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<TaskReportResponse>> {
        val result = taskReportUseCase.listByStatus(status, page, size)
        return ResponseEntity.ok(
            PageResponse(
                content = result.content.map { TaskReportResponse.from(it) },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages
            )
        )
    }

    @GetMapping("/{reportId}")
    fun getReport(@PathVariable reportId: String): ResponseEntity<TaskReportResponse> =
        taskReportUseCase.getById(reportId).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(TaskReportResponse.from(it)) }
        )

    @PostMapping("/{reportId}/correct")
    fun correctReport(
        @PathVariable reportId: String,
        @RequestBody request: CorrectTaskReportRequest
    ): ResponseEntity<TaskReportResponse> =
        taskReportUseCase.correct(
            CorrectTaskCommand(
                reportId = reportId,
                correctedTitle = request.correctedTitle,
                correctedOptions = request.correctedOptions,
                correctAnswerIndex = request.correctAnswerIndex
            )
        ).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(TaskReportResponse.from(it)) }
        )

    @PostMapping("/{reportId}/dismiss")
    fun dismissReport(@PathVariable reportId: String): ResponseEntity<TaskReportResponse> =
        taskReportUseCase.dismiss(reportId).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(TaskReportResponse.from(it)) }
        )

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
        ResponseEntity.status(httpStatus).build()
}
