package com.ureka.play4change.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "task-delivery")
class TaskDeliveryProperties {
    var taskRateMinutes: Int = MINUTES_PER_DAY
    var devMode: Boolean = false
    var devRateSeconds: Int = 120

    fun effectiveRateSeconds(): Long = if (devMode) devRateSeconds.toLong() else taskRateMinutes * 60L

    companion object {
        const val MINUTES_PER_DAY = 1440
    }
}
