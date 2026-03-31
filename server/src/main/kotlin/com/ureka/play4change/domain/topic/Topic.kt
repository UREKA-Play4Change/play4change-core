package com.ureka.play4change.domain.topic

import java.time.OffsetDateTime

data class Topic(
    val id: String,
    val title: String,
    val description: String,
    val contentSourceType: ContentSourceType,
    val contentSourceRef: String,
    val rawExtractedText: String?,
    val taskCount: Int,
    val subscriptionWindowDays: Int,
    val expiresAt: OffsetDateTime,
    val audienceLevel: AudienceLevel,
    val language: String,
    val status: TopicStatus,
    val createdBy: String,
    val createdAt: OffsetDateTime
) {
    fun canEnroll(): Boolean = status == TopicStatus.ACTIVE && !isExpired()

    fun isExpired(): Boolean = OffsetDateTime.now().isAfter(expiresAt)
}
