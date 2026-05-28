package com.ureka.play4change.domain.topic

data class TaskQuestionStats(
    val totalAttempts: Int,
    val successCount: Int,
    val successRate: Double,
    val avgPointsAwarded: Double
) {
    companion object {
        val ZERO = TaskQuestionStats(
            totalAttempts = 0,
            successCount = 0,
            successRate = 0.0,
            avgPointsAwarded = 0.0
        )
    }
}
