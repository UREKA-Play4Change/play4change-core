package com.ureka.play4change.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "task-delivery")
class TaskDeliveryProperties {
    var taskRateMinutes: Int = 1440
    var devMode: Boolean = false

    fun effectiveRateMinutes(): Int = if (devMode) 2 else taskRateMinutes
}
