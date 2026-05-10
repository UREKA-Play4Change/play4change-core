package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.ReportTaskCommand
import com.ureka.play4change.application.port.TaskReportUseCase
import com.ureka.play4change.error.AppError
import com.ureka.play4change.web.user.dto.ReportTaskRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tasks")
class TaskReportController(private val taskReportUseCase: TaskReportUseCase) {

    @PostMapping("/{taskId}/report")
    fun reportTask(
        @PathVariable taskId: String,
        @RequestBody request: ReportTaskRequest,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<Map<String, String>> {
        require(request.reason.isNotBlank()) { "reason must not be blank" }
        require(request.reason.length <= MAX_REASON_LENGTH) { "reason must be at most $MAX_REASON_LENGTH characters" }

        return taskReportUseCase.reportTask(
            ReportTaskCommand(
                userId = userId,
                taskTemplateId = taskId,
                reason = request.reason
            )
        ).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.status(HttpStatus.CREATED).body(mapOf("reportId" to it.id)) }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
        ResponseEntity.status(httpStatus).build()

    companion object {
        private const val MAX_REASON_LENGTH = 500
    }
}
