package com.ureka.play4change.application.port

import arrow.core.Either
import com.ureka.play4change.domain.topic.AudienceLevel
import com.ureka.play4change.domain.topic.Topic
import com.ureka.play4change.domain.topic.TopicStatus
import com.ureka.play4change.error.AppError
import java.time.OffsetDateTime

data class CreateUrlTopicCommand(
    val title: String,
    val description: String,
    val url: String,
    val taskCount: Int,
    val subscriptionWindowDays: Int,
    val audienceLevel: AudienceLevel,
    val language: String,
    val expiresAt: OffsetDateTime
)

data class CreatePdfTopicCommand(
    val title: String,
    val description: String,
    val pdfBytes: ByteArray,
    val fileName: String,
    val taskCount: Int,
    val subscriptionWindowDays: Int,
    val audienceLevel: AudienceLevel,
    val language: String,
    val expiresAt: OffsetDateTime
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CreatePdfTopicCommand) return false
        return title == other.title && description == other.description &&
            fileName == other.fileName && taskCount == other.taskCount &&
            subscriptionWindowDays == other.subscriptionWindowDays &&
            audienceLevel == other.audienceLevel && language == other.language &&
            expiresAt == other.expiresAt && pdfBytes.contentEquals(other.pdfBytes)
    }

    override fun hashCode(): Int = pdfBytes.contentHashCode() * 31 + title.hashCode()
}

interface TopicUseCase {
    fun createFromUrl(command: CreateUrlTopicCommand, adminId: String): Either<AppError, Topic>
    fun createFromPdf(command: CreatePdfTopicCommand, adminId: String): Either<AppError, Topic>
    fun getById(topicId: String): Either<AppError, Topic>
    fun listAll(status: TopicStatus?): Either<AppError, List<Topic>>
    fun regenerate(topicId: String, adminId: String): Either<AppError, Topic>
}
