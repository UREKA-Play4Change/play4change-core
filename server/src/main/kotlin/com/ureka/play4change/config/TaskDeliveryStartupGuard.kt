package com.ureka.play4change.config

import jakarta.annotation.PostConstruct
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class TaskDeliveryStartupGuard(
    private val properties: TaskDeliveryProperties,
    private val environment: Environment
) {
    @PostConstruct
    fun validate() {
        require(properties.taskRateMinutes >= 1) {
            "task-delivery.task-rate-minutes must be >= 1, got ${properties.taskRateMinutes}"
        }
        check(!(properties.devMode && environment.activeProfiles.contains("prod"))) {
            "task-delivery.dev-mode must not be enabled in the prod Spring profile"
        }
    }
}
