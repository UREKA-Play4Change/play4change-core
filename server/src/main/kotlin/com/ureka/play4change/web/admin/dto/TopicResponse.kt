package com.ureka.play4change.web.admin.dto

import com.ureka.play4change.application.port.TopicDetail
import com.ureka.play4change.domain.topic.Topic
import com.ureka.play4change.domain.topic.TopicPhaseLog
import java.time.OffsetDateTime

data class PhaseLogEntry(
    val fromPhase: String,
    val toPhase: String,
    val transitionedAt: OffsetDateTime,
    val durationMs: Long
)

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
    val contentTruncated: Boolean,

    // Phase 02, Task 2.8 — generation pipeline phase state
    val currentPhase: String,
    val phaseUpdatedAt: OffsetDateTime,
    val generationLog: List<PhaseLogEntry>? = null
) {
    companion object {
        fun from(topic: Topic): TopicResponse = from(topic, null)

        fun from(detail: TopicDetail): TopicResponse = from(detail.topic, detail.generationLog)

        private fun from(topic: Topic, log: List<TopicPhaseLog>?): TopicResponse = TopicResponse(
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
            contentTruncated = topic.rawExtractedText != null && topic.rawExtractedText.length >= 8000,
            currentPhase = topic.currentPhase.name,
            phaseUpdatedAt = topic.phaseUpdatedAt,
            generationLog = log?.map {
                PhaseLogEntry(
                    fromPhase = it.fromPhase.name,
                    toPhase = it.toPhase.name,
                    transitionedAt = it.transitionedAt,
                    durationMs = it.durationMs
                )
            }
        )
    }
}

data class TopicStats(
    val enrolledUsers: Int = 0,
    val completionRate: Double = 0.0,
    val averageScore: Double = 0.0,
    val activeUsers: Int = 0
)
