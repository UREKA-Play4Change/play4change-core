package com.ureka.play4change.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * Exposes [Clock.systemUTC] as a Spring bean so every service that needs the
 * current time receives it via constructor injection rather than calling
 * [java.time.OffsetDateTime.now] or [System.currentTimeMillis] directly.
 *
 * In tests, inject a [java.time.Clock.fixed] to get deterministic timestamps
 * without mocking static calls.
 */
@Configuration
class ClockConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
