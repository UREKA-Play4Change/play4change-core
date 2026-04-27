package com.ureka.play4change.web.admin

import com.ureka.play4change.application.port.CreatePdfTopicCommand
import com.ureka.play4change.application.port.CreateUrlTopicCommand
import com.ureka.play4change.application.port.TopicUseCase
import com.ureka.play4change.domain.topic.AudienceLevel
import com.ureka.play4change.domain.topic.TopicStatus
import com.ureka.play4change.error.AppError
import com.ureka.play4change.web.admin.dto.CreateUrlTopicRequest
import com.ureka.play4change.web.admin.dto.TopicResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime

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
            category = request.category,
            url = request.resolvedUrl(),
            taskCount = request.resolvedTaskCount(),
            subscriptionWindowDays = request.resolvedDurationDays(),
            audienceLevel = request.resolvedAudienceLevel(),
            language = request.language,
            expiresAt = request.resolvedExpiresAt()
        )
        return topicUseCase.createFromUrl(command, adminId).toResponse(HttpStatus.CREATED)
    }

    /**
     * Accepts the web frontend multipart shape:
     *   title, description, category, durationDays, difficulty, file
     *
     * Legacy CLI params (taskCount, subscriptionWindowDays, audienceLevel, language, expiresAt)
     * are optional with computed defaults so existing demo scripts continue to work.
     */
    @PostMapping("/pdf", consumes = ["multipart/form-data"])
    fun createFromPdf(
        @RequestParam title: String,
        @RequestParam description: String,
        @RequestParam(required = false, defaultValue = "") category: String,
        @RequestParam(required = false) durationDays: Int?,
        @RequestParam(required = false) subscriptionWindowDays: Int?,
        @RequestParam(required = false) difficulty: String?,
        @RequestParam(required = false) audienceLevel: AudienceLevel?,
        @RequestParam(required = false, defaultValue = "en") language: String,
        @RequestParam(required = false) taskCount: Int?,
        @RequestParam(required = false) expiresAt: OffsetDateTime?,
        @RequestPart file: MultipartFile,
        @AuthenticationPrincipal adminId: String
    ): ResponseEntity<TopicResponse> {
        val resolvedDuration = subscriptionWindowDays ?: durationDays ?: 5
        val resolvedAudience = audienceLevel
            ?: difficulty?.let { runCatching { AudienceLevel.valueOf(it) }.getOrNull() }
            ?: AudienceLevel.BEGINNER
        val resolvedTaskCount = taskCount ?: (resolvedDuration * 3)
        val resolvedExpiresAt = expiresAt ?: OffsetDateTime.now().plusDays(resolvedDuration.toLong() + 30)

        val command = CreatePdfTopicCommand(
            title = title,
            description = description,
            category = category,
            pdfBytes = file.bytes,
            fileName = file.originalFilename ?: "upload.pdf",
            taskCount = resolvedTaskCount,
            subscriptionWindowDays = resolvedDuration,
            audienceLevel = resolvedAudience,
            language = language,
            expiresAt = resolvedExpiresAt
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
