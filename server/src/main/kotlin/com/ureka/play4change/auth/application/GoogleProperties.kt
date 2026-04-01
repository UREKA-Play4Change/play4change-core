package com.ureka.play4change.auth.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "google.oauth")
data class GoogleProperties(
    val clientId: String = ""
)
