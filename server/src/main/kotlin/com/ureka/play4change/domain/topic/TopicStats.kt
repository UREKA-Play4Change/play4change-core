package com.ureka.play4change.domain.topic

data class TopicStats(
    val enrolledUsers: Int,
    val completionRate: Double,
    val totalScore: Int,
    val activeUsers: Int
)
