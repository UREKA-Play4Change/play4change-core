package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.SubmitAnswerCommand
import com.ureka.play4change.application.port.TaskUseCase
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.error.AppError
import com.ureka.play4change.web.user.dto.SubmitAnswerRequest
import com.ureka.play4change.web.user.dto.SubmitResultResponse
import com.ureka.play4change.web.user.dto.TaskResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/tasks")
class TaskController(
    private val taskUseCase: TaskUseCase,
    private val taskTemplateRepository: TaskTemplateRepository
) {

    @GetMapping("/today")
    fun getTodayTask(
        @RequestParam topicId: String,
        @AuthenticationPrincipal userId: String,
        @RequestHeader(value = "X-Timezone", required = false) timezone: String?
    ): ResponseEntity<TaskResponse> =
        taskUseCase.getTodayTask(userId, topicId, timezone).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { assignment ->
                val template = taskTemplateRepository.findById(assignment.taskTemplateId)
                    ?: return ResponseEntity.status(500).build()
                ResponseEntity.ok(TaskResponse.from(assignment, template))
            }
        )

    @PostMapping("/{assignmentId}/submit")
    fun submitAnswer(
        @PathVariable assignmentId: String,
        @RequestBody request: SubmitAnswerRequest,
        @AuthenticationPrincipal userId: String,
        @RequestHeader(value = "X-Timezone", required = false) timezone: String?
    ): ResponseEntity<SubmitResultResponse> =
        taskUseCase.submitAnswer(
            SubmitAnswerCommand(
                userId = userId,
                assignmentId = assignmentId,
                selectedOption = request.selectedOption,
                timezone = timezone
            )
        ).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(SubmitResultResponse.from(it)) }
        )

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
        ResponseEntity.status(httpStatus).build()
}
