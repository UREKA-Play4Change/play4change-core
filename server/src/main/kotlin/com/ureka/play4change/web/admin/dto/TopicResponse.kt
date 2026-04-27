package com.ureka.play4change.web.admin.dto

import com.ureka.play4change.domain.topic.Topic
import com.ureka.play4change.domain.topic.TopicStatus
import java.time.OffsetDateTime

/**
 * Unified topic response understood by both the web admin frontend and legacy CLI/demo clients.
 *
 * Web frontend field mapping:
 *   difficulty   ← audienceLevel.name
 *   durationDays ← subscriptionWindowDays
 *   category     ← category
 *   status       ← DRAFT is exposed as "PENDING" to match the frontend's four-state model
 *   stats        ← zeroed stub; wire to real enrollment queries in a future phase
 */
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

    val stats: TopicStats = TopicStats()
) {
    companion object {
        fun from(topic: Topic) = TopicResponse(
            id = topic.id,
            title = topic.title,
            description = topic.description,
            category = topic.category,
            // DRAFT is the internal initial state; the web frontend calls it PENDING.
            status = if (topic.status == TopicStatus.DRAFT) "PENDING" else topic.status.name,
            createdAt = topic.createdAt,
            durationDays = topic.subscriptionWindowDays,
            difficulty = topic.audienceLevel.name,
            contentSourceType = topic.contentSourceType.name,
            taskCount = topic.taskCount,
            subscriptionWindowDays = topic.subscriptionWindowDays,
            audienceLevel = topic.audienceLevel.name,
            language = topic.language,
            expiresAt = topic.expiresAt,
            createdBy = topic.createdBy
        )
    }
}

data class TopicStats(
    val enrolledUsers: Int = 0,
    val completionRate: Double = 0.0,
    val averageScore: Double = 0.0,
    val activeUsers: Int = 0
)
