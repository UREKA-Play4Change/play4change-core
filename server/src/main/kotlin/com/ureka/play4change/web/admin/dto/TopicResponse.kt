package com.ureka.play4change.web.admin.dto

import com.ureka.play4change.domain.topic.Topic
import java.time.OffsetDateTime

data class TopicResponse(
    val id: String,
    val title: String,
    val description: String,
    val contentSourceType: String,
    val taskCount: Int,
    val subscriptionWindowDays: Int,
    val audienceLevel: String,
    val language: String,
    val status: String,
    val expiresAt: OffsetDateTime,
    val createdBy: String,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(topic: Topic) = TopicResponse(
            id = topic.id,
            title = topic.title,
            description = topic.description,
            contentSourceType = topic.contentSourceType.name,
            taskCount = topic.taskCount,
            subscriptionWindowDays = topic.subscriptionWindowDays,
            audienceLevel = topic.audienceLevel.name,
            language = topic.language,
            status = topic.status.name,
            expiresAt = topic.expiresAt,
            createdBy = topic.createdBy,
            createdAt = topic.createdAt
        )
    }
}
