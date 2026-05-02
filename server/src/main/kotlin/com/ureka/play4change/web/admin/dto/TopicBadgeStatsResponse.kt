package com.ureka.play4change.web.admin.dto

import com.ureka.play4change.application.port.TopicBadgeStatsDto
import java.time.OffsetDateTime

data class RecentEarnerResponse(
    val userId: String,
    val earnedAt: OffsetDateTime
)

data class TopicBadgeStatsResponse(
    val totalIssued: Int,
    val enrolledCount: Int,
    val earnedPercentage: Double,
    val recentEarners: List<RecentEarnerResponse>
) {
    companion object {
        fun from(dto: TopicBadgeStatsDto) = TopicBadgeStatsResponse(
            totalIssued = dto.totalIssued,
            enrolledCount = dto.enrolledCount,
            earnedPercentage = dto.earnedPercentage,
            recentEarners = dto.recentEarners.map { RecentEarnerResponse(it.userId, it.earnedAt) }
        )
    }
}
