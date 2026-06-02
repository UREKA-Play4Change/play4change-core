package com.ureka.play4change.web.admin.dto

import java.time.OffsetDateTime

data class CreatePdfTopicRequest(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val difficulty: String = "BEGINNER",
    val language: String = "en",
    val taskCount: Int? = null,
    val expiresAt: OffsetDateTime? = null
)
