package com.ureka.play4change.web.admin

import com.ureka.play4change.application.port.CreatePdfTopicCommand
import com.ureka.play4change.application.port.CreateUrlTopicCommand
import com.ureka.play4change.application.port.TopicUseCase
import com.ureka.play4change.domain.topic.TopicStatus
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.error.client.Conflict
import com.ureka.play4change.error.client.NotFound
import com.ureka.play4change.error.server.InternalServerError
import com.ureka.play4change.web.admin.dto.CreateUrlTopicRequest
import com.ureka.play4change.web.admin.dto.TopicResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/admin/topics")
class TopicController(private val topicUseCase: TopicUseCase) {

    @PostMapping
    fun createFromUrl(
        @RequestBody request: CreateUrlTopicRequest,
        @AuthenticationPrincipal adminId: String
    ): ResponseEntity<TopicResponse> {
        val command = CreateUrlTopicCommand(
            title = request.title,
            description = request.description,
            url = request.url,
            taskCount = request.taskCount,
            subscriptionWindowDays = request.subscriptionWindowDays,
            audienceLevel = request.audienceLevel,
            language = request.language,
            expiresAt = request.expiresAt
        )
        return topicUseCase.createFromUrl(command, adminId).toResponse(HttpStatus.CREATED)
    }

    @PostMapping("/pdf", consumes = ["multipart/form-data"])
    fun createFromPdf(
        @RequestParam title: String,
        @RequestParam description: String,
        @RequestParam taskCount: Int,
        @RequestParam subscriptionWindowDays: Int,
        @RequestParam audienceLevel: com.ureka.play4change.domain.topic.AudienceLevel,
        @RequestParam language: String,
        @RequestParam expiresAt: java.time.OffsetDateTime,
        @RequestPart file: MultipartFile,
        @AuthenticationPrincipal adminId: String
    ): ResponseEntity<TopicResponse> {
        val command = CreatePdfTopicCommand(
            title = title,
            description = description,
            pdfBytes = file.bytes,
            fileName = file.originalFilename ?: "upload.pdf",
            taskCount = taskCount,
            subscriptionWindowDays = subscriptionWindowDays,
            audienceLevel = audienceLevel,
            language = language,
            expiresAt = expiresAt
        )
        return topicUseCase.createFromPdf(command, adminId).toResponse(HttpStatus.CREATED)
    }

    @GetMapping
    fun listAll(
        @RequestParam(required = false) status: TopicStatus?
    ): ResponseEntity<List<TopicResponse>> =
        topicUseCase.listAll(status).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(it.map(TopicResponse::from)) }
        )

    @GetMapping("/{id}")
    fun getById(@PathVariable id: String): ResponseEntity<TopicResponse> =
        topicUseCase.getById(id).toResponse(HttpStatus.OK)

    @PostMapping("/{id}/regenerate")
    fun regenerate(
        @PathVariable id: String,
        @AuthenticationPrincipal adminId: String
    ): ResponseEntity<TopicResponse> =
        topicUseCase.regenerate(id, adminId).toResponse(HttpStatus.OK)

    private fun arrow.core.Either<AppError, com.ureka.play4change.domain.topic.Topic>.toResponse(
        successStatus: HttpStatus
    ): ResponseEntity<TopicResponse> = fold(
        ifLeft = { it.toErrorResponse() },
        ifRight = { ResponseEntity.status(successStatus).body(TopicResponse.from(it)) }
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppError.toErrorResponse(): ResponseEntity<T> =
        ResponseEntity.status(httpStatus).build()
}
