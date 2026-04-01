package com.ureka.play4change.application.topic

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.ureka.play4change.application.port.ContentExtractorPort
import com.ureka.play4change.application.port.CreatePdfTopicCommand
import com.ureka.play4change.application.port.CreateUrlTopicCommand
import com.ureka.play4change.application.port.FileStoragePort
import com.ureka.play4change.application.port.TopicUseCase
import com.ureka.play4change.domain.topic.ContentSourceType
import com.ureka.play4change.domain.topic.Topic
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.domain.topic.TopicStatus
import com.ureka.play4change.error.AppError
import com.ureka.play4change.error.client.BadRequest
import com.ureka.play4change.error.client.Conflict
import com.ureka.play4change.error.client.NotFound
import com.ureka.play4change.error.server.InternalServerError
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class TopicManagementService(
    private val topicRepository: TopicRepository,
    private val fileStoragePort: FileStoragePort,
    private val contentExtractorPort: ContentExtractorPort,
    private val orchestrator: TaskGenerationOrchestrator
) : TopicUseCase {

    private val log = LoggerFactory.getLogger(TopicManagementService::class.java)

    override fun createFromUrl(command: CreateUrlTopicCommand, adminId: String): Either<AppError, Topic> = either {
        ensure(command.subscriptionWindowDays >= 7) {
            BadRequest.InvalidField("subscriptionWindowDays", "must be at least 7")
        }
        ensure(command.taskCount > 0) {
            BadRequest.InvalidField("taskCount", "must be greater than 0")
        }

        val rawText = try {
            contentExtractorPort.extractFromUrl(command.url)
        } catch (ex: Exception) {
            log.warn("Failed to fetch URL content for topic creation: {}", ex.message)
            raise(BadRequest.InvalidField("url", "could not fetch content: ${ex.message}"))
        }

        ensure(rawText.isNotBlank()) {
            BadRequest.InvalidField("url", "page returned no extractable text")
        }

        val topicId = UUID.randomUUID().toString()
        val storageKey = "topics/$topicId/content.txt"
        val contentRef = try {
            fileStoragePort.uploadFile(storageKey, rawText.toByteArray(Charsets.UTF_8), "text/plain")
        } catch (ex: Exception) {
            log.error("MinIO upload failed for topic {}: {}", topicId, ex.message)
            raise(InternalServerError.UnexpectedException)
        }

        val topic = topicRepository.save(
            Topic(
                id = topicId,
                title = command.title,
                description = command.description,
                contentSourceType = ContentSourceType.URL,
                contentSourceRef = contentRef,
                rawExtractedText = rawText,
                taskCount = command.taskCount,
                subscriptionWindowDays = command.subscriptionWindowDays,
                expiresAt = command.expiresAt,
                audienceLevel = command.audienceLevel,
                language = command.language,
                status = TopicStatus.DRAFT,
                createdBy = adminId,
                createdAt = OffsetDateTime.now()
            )
        )

        log.info("Topic {} created from URL by admin {}", topicId, adminId)
        orchestrator.generateAsync(topicId)
        topic
    }

    override fun createFromPdf(command: CreatePdfTopicCommand, adminId: String): Either<AppError, Topic> = either {
        ensure(command.subscriptionWindowDays >= 7) {
            BadRequest.InvalidField("subscriptionWindowDays", "must be at least 7")
        }
        ensure(command.taskCount > 0) {
            BadRequest.InvalidField("taskCount", "must be greater than 0")
        }
        ensure(command.pdfBytes.isNotEmpty()) {
            BadRequest.InvalidField("file", "PDF file is empty")
        }

        val rawText = try {
            contentExtractorPort.extractFromPdf(command.pdfBytes)
        } catch (ex: Exception) {
            log.warn("PDF extraction failed: {}", ex.message)
            raise(BadRequest.InvalidField("file", "could not extract text from PDF: ${ex.message}"))
        }

        ensure(rawText.isNotBlank()) {
            BadRequest.InvalidField("file", "PDF contains no extractable text")
        }

        val topicId = UUID.randomUUID().toString()
        val storageKey = "topics/$topicId/content.pdf"
        val contentRef = try {
            fileStoragePort.uploadFile(storageKey, command.pdfBytes, "application/pdf")
        } catch (ex: Exception) {
            log.error("MinIO upload failed for topic {}: {}", topicId, ex.message)
            raise(InternalServerError.UnexpectedException)
        }

        val topic = topicRepository.save(
            Topic(
                id = topicId,
                title = command.title,
                description = command.description,
                contentSourceType = ContentSourceType.PDF,
                contentSourceRef = contentRef,
                rawExtractedText = rawText,
                taskCount = command.taskCount,
                subscriptionWindowDays = command.subscriptionWindowDays,
                expiresAt = command.expiresAt,
                audienceLevel = command.audienceLevel,
                language = command.language,
                status = TopicStatus.DRAFT,
                createdBy = adminId,
                createdAt = OffsetDateTime.now()
            )
        )

        log.info("Topic {} created from PDF '{}' by admin {}", topicId, command.fileName, adminId)
        orchestrator.generateAsync(topicId)
        topic
    }

    override fun getById(topicId: String): Either<AppError, Topic> = either {
        ensureNotNull(topicRepository.findById(topicId)) {
            NotFound.ResourceNotFound("Topic", topicId)
        }
    }

    override fun listAll(status: TopicStatus?): Either<AppError, List<Topic>> = either {
        if (status != null) topicRepository.findByStatus(status)
        else topicRepository.findAll()
    }

    override fun regenerate(topicId: String, adminId: String): Either<AppError, Topic> = either {
        val topic = ensureNotNull(topicRepository.findById(topicId)) {
            NotFound.ResourceNotFound("Topic", topicId)
        }
        ensure(topic.status != TopicStatus.GENERATING) {
            Conflict.ConcurrentModification
        }

        log.info("Regeneration of topic {} requested by admin {}", topicId, adminId)
        orchestrator.generateAsync(topicId)
        topic
    }
}
