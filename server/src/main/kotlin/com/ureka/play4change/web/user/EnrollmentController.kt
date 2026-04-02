package com.ureka.play4change.web.user

import com.ureka.play4change.application.port.EnrollCommand
import com.ureka.play4change.application.port.EnrollmentUseCase
import com.ureka.play4change.error.AppError
import com.ureka.play4change.web.user.dto.EnrollmentResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/topics")
class EnrollmentController(private val enrollmentUseCase: EnrollmentUseCase) {

    @PostMapping("/{topicId}/enroll")
    fun enroll(
        @PathVariable topicId: String,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<EnrollmentResponse> =
        enrollmentUseCase.enroll(EnrollCommand(userId, topicId)).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.status(HttpStatus.CREATED).body(EnrollmentResponse.from(it)) }
        )

    @GetMapping("/{topicId}/enrollment")
    fun getEnrollment(
        @PathVariable topicId: String,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<EnrollmentResponse> =
        enrollmentUseCase.getEnrollment(userId, topicId).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(EnrollmentResponse.from(it)) }
        )

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
        ResponseEntity.status(httpStatus).build()
}
