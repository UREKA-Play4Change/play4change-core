package com.ureka.play4change.web.admin

import com.ureka.play4change.application.port.CreatePdfTopicCommand
import com.ureka.play4change.application.port.CreateUrlTopicCommand
import com.ureka.play4change.application.port.TopicUseCase
import com.ureka.play4change.domain.topic.AudienceLevel
import com.ureka.play4change.domain.topic.TopicStatus
import com.ureka.play4change.error.AppError
import com.ureka.play4change.web.admin.dto.CreatePdfTopicRequest
import com.ureka.play4change.web.admin.dto.CreateUrlTopicRequest
import com.ureka.play4change.web.admin.dto.PageResponse
import com.ureka.play4change.web.admin.dto.TopicResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.OffsetDateTime

@RestController
@RequestMapping("/admin/topics")
class TopicController(
    private val topicUseCase: TopicUseCase,
    private val ssePublisher: SseTopicEventPublisher
) {

    @PostMapping
    fun createFromUrl(
        @RequestBody request: CreateUrlTopicRequest,
        @AuthenticationPrincipal adminId: String
    ): ResponseEntity<TopicResponse> {
        val url = request.urls.first()
        val duration = request.durationDays
        val taskCount = request.taskCount ?: (request.durationDays * 3)
        val expiresAt = request.expiresAt ?: OffsetDateTime.now().plusDays((request.durationDays + 30).toLong())
        val audience = runCatching { AudienceLevel.valueOf(request.difficulty.uppercase()) }
            .getOrDefault(AudienceLevel.BEGINNER)

        val command = CreateUrlTopicCommand(
            title = request.title,
            description = request.description,
            category = request.category,
            url = url,
            taskCount = taskCount,
            subscriptionWindowDays = duration,
            audienceLevel = audience,
            language = request.language,
            expiresAt = expiresAt
        )
        return topicUseCase.createFromUrl(command, adminId).toResponse(HttpStatus.CREATED)
    }

    @PostMapping("/pdf", consumes = ["multipart/form-data"])
    fun createFromPdf(
        @ModelAttribute request: CreatePdfTopicRequest,
        @RequestPart file: MultipartFile,
        @AuthenticationPrincipal adminId: String
    ): ResponseEntity<TopicResponse> {
        val taskCount = request.taskCount ?: (request.durationDays * 3)
        val expiresAt = request.expiresAt ?: OffsetDateTime.now().plusDays((request.durationDays + 30).toLong())
        val audience = runCatching { AudienceLevel.valueOf(request.difficulty.uppercase()) }
            .getOrDefault(AudienceLevel.BEGINNER)

        val command = CreatePdfTopicCommand(
            title = request.title,
            description = request.description,
            category = request.category,
            pdfBytes = file.bytes,
            fileName = file.originalFilename ?: "upload.pdf",
            taskCount = taskCount,
            subscriptionWindowDays = request.durationDays,
            audienceLevel = audience,
            language = request.language,
            expiresAt = expiresAt
        )
        return topicUseCase.createFromPdf(command, adminId).toResponse(HttpStatus.CREATED)
    }

    @GetMapping
    fun listAll(
        @RequestParam status: String? = null,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageResponse<TopicResponse>> {
        val result = topicUseCase.listAll(status, page, size)
        return ResponseEntity.ok(
            PageResponse(
                content = result.content.map { TopicResponse.from(it) },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages
            )
        )
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: String): ResponseEntity<TopicResponse> =
        topicUseCase.getByIdWithLog(id).fold(
            ifLeft = { it.toErrorResponse() },
            ifRight = { ResponseEntity.ok(TopicResponse.from(it)) }
        )

    @GetMapping("/{id}/progress", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamProgress(
        @PathVariable id: String,
        response: HttpServletResponse
    ): SseEmitter {
        response.setHeader("X-Accel-Buffering", "no")
        response.setHeader("Cache-Control", "no-cache")
        val emitter = ssePublisher.register(id)
        topicUseCase.getByIdWithLog(id).fold(
            ifLeft = { ssePublisher.failed(id, "Topic not found") },
            ifRight = { detail ->
                when (detail.topic.status) {
                    TopicStatus.ACTIVE -> ssePublisher.completed(id, 0)
                    TopicStatus.FAILED -> ssePublisher.failed(id, "Generation failed")
                    else -> { /* pipeline running — orchestrator drives events */ }
                }
            }
        )
        return emitter
    }

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
