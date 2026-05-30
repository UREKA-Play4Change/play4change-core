package com.ureka.play4change.web.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SubmitPhotoRequest(
    @field:NotBlank @field:Size(max = 2000) val photoUrl: String
)
