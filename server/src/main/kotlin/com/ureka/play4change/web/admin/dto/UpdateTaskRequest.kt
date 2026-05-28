package com.ureka.play4change.web.admin.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateTaskRequest(
    @field:NotBlank val title: String,
    @field:NotBlank val description: String,
    val hint: String?,
    @field:Size(min = 2, message = "options must have at least 2 entries") val options: List<String>?,
    val correctAnswer: Int?
)
