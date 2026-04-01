package com.ureka.play4change.auth.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "resend")
data class ResendProperties(
    val apiKey: String = "",
    val from: String = "noreply@play4change.app"
)
