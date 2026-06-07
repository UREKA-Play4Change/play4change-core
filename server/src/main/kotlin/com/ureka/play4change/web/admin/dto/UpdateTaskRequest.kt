package com.ureka.play4change.web.admin.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateTaskRequest(
    @field:NotBlank @field:Size(max = 500) val title: String,
    @field:NotBlank @field:Size(max = 2000) val description: String,
    @field:Size(max = 500) val hint: String?,
    @field:Size(min = 2, max = 10, message = "options must have between 2 and 10 entries") val options: List<@Size(max = 500) String>?,
    @field:Min(0) @field:Max(9) val correctAnswer: Int?
)
