package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.StruggleUseCase
import com.ureka.play4change.application.port.SubmitAdaptiveTaskCommand
import com.ureka.play4change.error.AppError
import com.ureka.play4change.web.user.dto.AdaptiveTaskResponse
import com.ureka.play4change.web.user.dto.StruggleSessionResponse
import com.ureka.play4change.web.user.dto.SubmitAdaptiveTaskRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/struggle")
class StruggleController(private val struggleUseCase: StruggleUseCase) {

    @GetMapping("/enrollment/{enrollmentId}")
    fun getSession(
        @PathVariable enrollmentId: String,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<StruggleSessionResponse> =
        struggleUseCase.getSession(userId, enrollmentId).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(StruggleSessionResponse.from(it)) }
        )

    @PostMapping("/{sessionId}/tasks/{taskId}/submit")
    fun submitAdaptiveTask(
        @PathVariable sessionId: String,
        @PathVariable taskId: String,
        @RequestBody request: SubmitAdaptiveTaskRequest,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<AdaptiveTaskResponse> =
        struggleUseCase.submitAdaptiveTask(
            SubmitAdaptiveTaskCommand(
                userId = userId,
                sessionId = sessionId,
                taskId = taskId,
                selectedOption = request.selectedOption
            )
        ).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(AdaptiveTaskResponse.from(it.task)) }
        )

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
        ResponseEntity.status(httpStatus).build()
}
