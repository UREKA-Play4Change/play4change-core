package com.ureka.play4change.auth.adapter.inbound.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class AuthExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<MessageResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(MessageResponse(ex.message ?: "Bad request"))

    @ExceptionHandler(SecurityException::class)
    fun handleSecurityViolation(ex: SecurityException): ResponseEntity<MessageResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(MessageResponse(ex.message ?: "Unauthorized"))

    @ExceptionHandler(IllegalStateException::class)
    fun handleUpstreamError(ex: IllegalStateException): ResponseEntity<MessageResponse> =
        // Do not forward ex.message — may contain upstream URLs or provider error text
        ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(MessageResponse("Upstream service error"))

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedBody(ex: HttpMessageNotReadableException): ResponseEntity<MessageResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(MessageResponse("Malformed request body"))
}
