package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.ExplanationUseCase
import com.ureka.play4change.application.port.ResolveExplanationCommand
import com.ureka.play4change.application.port.SendExplanationMessageCommand
import com.ureka.play4change.error.AppError
import com.ureka.play4change.web.user.dto.ExplanationMessageResponse
import com.ureka.play4change.web.user.dto.ExplanationSessionResponse
import com.ureka.play4change.web.user.dto.SendExplanationMessageRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/explanation")
class ExplanationController(private val explanationUseCase: ExplanationUseCase) {

    @GetMapping("/{sessionId}")
    fun getSession(
        @PathVariable sessionId: String,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<ExplanationSessionResponse> =
        explanationUseCase.getSession(userId, sessionId).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(ExplanationSessionResponse.from(it)) }
        )

    @PostMapping("/{sessionId}/message")
    fun sendMessage(
        @PathVariable sessionId: String,
        @Valid @RequestBody request: SendExplanationMessageRequest,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<ExplanationMessageResponse> =
        explanationUseCase.sendMessage(
            SendExplanationMessageCommand(
                userId = userId,
                sessionId = sessionId,
                content = request.content
            )
        ).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(ExplanationMessageResponse.from(it)) }
        )

    @PostMapping("/{sessionId}/resolve")
    fun resolve(
        @PathVariable sessionId: String,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<Void> =
        explanationUseCase.resolve(
            ResolveExplanationCommand(userId = userId, sessionId = sessionId)
        ).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok().build() }
        )

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
        ResponseEntity.status(httpStatus).build()
}
