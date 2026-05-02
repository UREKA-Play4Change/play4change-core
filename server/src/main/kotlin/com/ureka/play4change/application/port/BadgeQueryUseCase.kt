package com.ureka.play4change.application.port

import java.time.OffsetDateTime

data class UserBadgeDto(
    val microCompetenceName: String,
    val description: String,
    val topicTitle: String,
    val earnedAt: OffsetDateTime
)

data class RecentEarnerDto(
    val userId: String,
    val earnedAt: OffsetDateTime
)

data class TopicBadgeStatsDto(
    val totalIssued: Int,
    val enrolledCount: Int,
    val earnedPercentage: Double,
    val recentEarners: List<RecentEarnerDto>
)

interface BadgeQueryUseCase {
    fun getUserBadges(userId: String): List<UserBadgeDto>
    fun getTopicBadgeStats(topicId: String): TopicBadgeStatsDto
}
