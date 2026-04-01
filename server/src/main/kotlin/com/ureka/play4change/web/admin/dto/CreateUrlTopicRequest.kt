package com.ureka.play4change.web.admin.dto

import com.ureka.play4change.domain.topic.AudienceLevel
import java.time.OffsetDateTime

data class CreateUrlTopicRequest(
    val title: String,
    val description: String,
    val url: String,
    val taskCount: Int,
    val subscriptionWindowDays: Int,
    val audienceLevel: AudienceLevel,
    val language: String,
    val expiresAt: OffsetDateTime
)
