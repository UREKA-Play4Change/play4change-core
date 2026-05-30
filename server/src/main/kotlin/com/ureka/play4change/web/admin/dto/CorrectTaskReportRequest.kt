package com.ureka.play4change.web.admin.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class CorrectTaskReportRequest(
    @field:NotBlank @field:Size(max = 500) val correctedTitle: String,
    @field:NotEmpty @field:Size(min = 2, max = 10) val correctedOptions: List<String>,
    val correctAnswerIndex: Int
)
