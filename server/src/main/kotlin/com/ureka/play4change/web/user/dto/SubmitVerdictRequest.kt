package com.ureka.play4change.web.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SubmitVerdictRequest(
    @field:NotBlank @field:Size(max = 50) val verdict: String,
    @field:Size(max = 500) val comment: String?
)
