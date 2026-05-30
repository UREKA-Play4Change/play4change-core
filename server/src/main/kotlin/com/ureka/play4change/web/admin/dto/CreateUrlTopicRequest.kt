package com.ureka.play4change.web.admin.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

data class CreateUrlTopicRequest(
    @field:NotBlank @field:Size(max = 200) val title: String,
    @field:NotBlank @field:Size(max = 1000) val description: String,
    @field:Size(max = 100) val category: String = "",
    @field:NotEmpty @field:Size(max = 10) val urls: List<String>,
    @field:Min(1) @field:Max(365) val durationDays: Int,
    @field:Size(max = 50) val difficulty: String = "BEGINNER",
    @field:Size(max = 10) val language: String = "en",
    val taskCount: Int? = null,
    val expiresAt: OffsetDateTime? = null
)
