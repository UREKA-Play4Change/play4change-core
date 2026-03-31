package com.ureka.play4change.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "topics")
class TopicEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(name = "content_source_type", nullable = false, length = 10)
    val contentSourceType: String,

    @Column(name = "content_source_ref", nullable = false, columnDefinition = "TEXT")
    val contentSourceRef: String,

    @Column(name = "raw_extracted_text", columnDefinition = "TEXT")
    val rawExtractedText: String? = null,

    @Column(name = "task_count", nullable = false)
    val taskCount: Int,

    @Column(name = "subscription_window_days", nullable = false)
    val subscriptionWindowDays: Int = 7,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: OffsetDateTime,

    @Column(name = "audience_level", nullable = false, length = 20)
    val audienceLevel: String = "BEGINNER",

    @Column(nullable = false, length = 10)
    val language: String = "en",

    @Column(nullable = false, length = 15)
    var status: String = "DRAFT",

    @Column(name = "created_by", nullable = false, length = 36)
    val createdBy: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
