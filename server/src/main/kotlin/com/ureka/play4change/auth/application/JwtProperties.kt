package com.ureka.play4change.auth.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val accessTtlMinutes: Long = 15,
    val refreshTtlDays: Long = 7
)
