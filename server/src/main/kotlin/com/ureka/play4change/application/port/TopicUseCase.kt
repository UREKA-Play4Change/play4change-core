package com.ureka.play4change.application.port

import arrow.core.Either
import com.ureka.play4change.domain.topic.AudienceLevel
import com.ureka.play4change.domain.topic.PageResult
import com.ureka.play4change.domain.topic.Topic
import com.ureka.play4change.domain.topic.TopicPhaseLog
import com.ureka.play4change.domain.topic.TopicStatus
import com.ureka.play4change.error.AppError
import java.time.OffsetDateTime

data class CreateUrlTopicCommand(
    val title: String,
    val description: String,
    val category: String,
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
    val category: String,
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
            category == other.category && fileName == other.fileName &&
            taskCount == other.taskCount && subscriptionWindowDays == other.subscriptionWindowDays &&
            audienceLevel == other.audienceLevel && language == other.language &&
            expiresAt == other.expiresAt && pdfBytes.contentEquals(other.pdfBytes)
    }

    override fun hashCode(): Int = pdfBytes.contentHashCode() * 31 + title.hashCode()
}

data class TopicDetail(
    val topic: Topic,
    val generationLog: List<TopicPhaseLog>
)

interface TopicUseCase {
    fun createFromUrl(command: CreateUrlTopicCommand, adminId: String): Either<AppError, Topic>
    fun createFromPdf(command: CreatePdfTopicCommand, adminId: String): Either<AppError, Topic>
    fun getById(topicId: String): Either<AppError, Topic>
    fun getByIdWithLog(topicId: String): Either<AppError, TopicDetail>
    fun listAll(statusFilter: String?, page: Int, size: Int): PageResult<Topic>
    fun regenerate(topicId: String, adminId: String): Either<AppError, Topic>
    fun markFailed(topicId: String): Either<AppError, Topic>
}
