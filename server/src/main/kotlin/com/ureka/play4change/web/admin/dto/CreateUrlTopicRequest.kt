package com.ureka.play4change.web.admin.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

data class CreateUrlTopicRequest(
    @field:NotBlank @field:Size(max = 200) val title: String,
    @field:NotBlank @field:Size(max = 1000) val description: String,
    @field:Size(max = 100) val category: String = "",
    @field:NotBlank val url: String,
    @field:Size(max = 50) val difficulty: String = "BEGINNER",
    @field:Size(max = 10) val language: String = "en",
    val taskCount: Int? = null,
    val expiresAt: OffsetDateTime? = null
)
