package com.ureka.play4change.web.admin.dto

import com.ureka.play4change.domain.topic.Topic
import java.time.OffsetDateTime

data class TopicResponse(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val status: String,
    val createdAt: OffsetDateTime,

    // Aliased fields — match web frontend field names
    val durationDays: Int,
    val difficulty: String,

    // Legacy fields — kept so existing demo/CLI clients are not broken
    val contentSourceType: String,
    val taskCount: Int,
    val subscriptionWindowDays: Int,
    val audienceLevel: String,
    val language: String,
    val expiresAt: OffsetDateTime,
    val createdBy: String,

    val stats: TopicStats? = null,
    val contentTruncated: Boolean
) {
    companion object {
        fun from(topic: Topic) = TopicResponse(
            id = topic.id,
            title = topic.title,
            description = topic.description,
            category = topic.category,
            status = topic.status.name,
            createdAt = topic.createdAt,
            durationDays = topic.subscriptionWindowDays,
            difficulty = topic.audienceLevel.name,
            contentSourceType = topic.contentSourceType.name,
            taskCount = topic.taskCount,
            subscriptionWindowDays = topic.subscriptionWindowDays,
            audienceLevel = topic.audienceLevel.name,
            language = topic.language,
            expiresAt = topic.expiresAt,
            createdBy = topic.createdBy,
            stats = null,
            contentTruncated = topic.rawExtractedText != null && topic.rawExtractedText.length >= 8000
        )
    }
}

data class TopicStats(
    val enrolledUsers: Int = 0,
    val completionRate: Double = 0.0,
    val averageScore: Double = 0.0,
    val activeUsers: Int = 0
)
