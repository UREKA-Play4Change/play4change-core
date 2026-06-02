package com.ureka.play4change.auth.adapter.inbound.web

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class MagicLinkRequest(
    @field:NotBlank @field:Email val email: String
)

data class MagicLinkVerifyRequest(
    @field:NotBlank val token: String
)

data class RefreshRequest(
    @field:NotBlank val refreshToken: String
)
