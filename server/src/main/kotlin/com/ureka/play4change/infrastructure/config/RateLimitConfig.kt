package com.ureka.play4change.infrastructure.config

import io.github.bucket4j.TimeMeter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RateLimitConfig {

    /** Production clock — uses the system clock in millisecond precision. */
    @Bean
    fun timeMeter(): TimeMeter = TimeMeter.SYSTEM_MILLISECONDS
}
