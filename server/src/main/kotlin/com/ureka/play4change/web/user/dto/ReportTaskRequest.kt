package com.ureka.play4change.web.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ReportTaskRequest(
    @field:NotBlank @field:Size(max = 500) val reason: String
)
