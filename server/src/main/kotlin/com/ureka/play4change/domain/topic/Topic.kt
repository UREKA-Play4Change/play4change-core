package com.ureka.play4change.domain.topic

import java.time.OffsetDateTime

data class Topic(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val contentSourceType: ContentSourceType,
    val contentSourceRef: String,
    val rawExtractedText: String?,
    val taskCount: Int,
    val expiresAt: OffsetDateTime,
    val audienceLevel: AudienceLevel,
    val language: String,
    val status: TopicStatus,
    val createdBy: String,
    val createdAt: OffsetDateTime,
    val version: Long = 0,
    val statusUpdatedAt: OffsetDateTime = OffsetDateTime.now(),
    val currentPhase: GenerationPhase = GenerationPhase.INGESTION,
    val phaseUpdatedAt: OffsetDateTime = OffsetDateTime.now()
) {
    fun canEnroll(): Boolean = status == TopicStatus.ACTIVE && !isExpired()

    fun isExpired(): Boolean = OffsetDateTime.now().isAfter(expiresAt)
}
