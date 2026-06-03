package com.ureka.play4change.domain.topic

data class TaskQuestionStats(
    val totalAttempts: Int,
    val successCount: Int,
    val successRate: Double,
    val avgPointsAwarded: Double,
    val struggleTriggerCount: Int = 0
) {
    companion object {
        val ZERO = TaskQuestionStats(
            totalAttempts = 0,
            successCount = 0,
            successRate = 0.0,
            avgPointsAwarded = 0.0,
            struggleTriggerCount = 0
        )
    }
}
