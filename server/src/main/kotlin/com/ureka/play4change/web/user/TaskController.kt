package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.SubmitAnswerCommand
import com.ureka.play4change.application.port.SubmitPhotoCommand
import com.ureka.play4change.application.port.TaskUseCase
import com.ureka.play4change.error.AppError
import com.ureka.play4change.web.user.dto.SubmitAnswerRequest
import com.ureka.play4change.web.user.dto.SubmitPhotoRequest
import com.ureka.play4change.web.user.dto.SubmitResultResponse
import com.ureka.play4change.web.user.dto.TaskResponse
import com.ureka.play4change.web.user.dto.TodoSubmitResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/tasks")
class TaskController(
    private val taskUseCase: TaskUseCase
) {

    @GetMapping("/today")
    fun getTodayTask(
        @RequestParam topicId: String,
        @AuthenticationPrincipal userId: String,
        @RequestHeader(value = "X-Timezone", required = false) timezone: String?
    ): ResponseEntity<TaskResponse> =
        taskUseCase.getTodayTask(userId, topicId, timezone).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(TaskResponse.from(it.assignment, it.template)) }
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

    @PostMapping("/{assignmentId}/submit-photo")
    fun submitPhoto(
        @PathVariable assignmentId: String,
        @RequestBody request: SubmitPhotoRequest,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<TodoSubmitResponse> =
        taskUseCase.submitPhoto(
            SubmitPhotoCommand(
                userId = userId,
                assignmentId = assignmentId,
                photoUrl = request.photoUrl
            )
        ).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(TodoSubmitResponse.from(it)) }
        )

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
        ResponseEntity.status(httpStatus).build()
}
