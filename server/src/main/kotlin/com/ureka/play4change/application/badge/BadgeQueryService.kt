package com.ureka.play4change.application.badge

import com.ureka.play4change.application.port.BadgeQueryUseCase
import com.ureka.play4change.application.port.RecentEarnerDto
import com.ureka.play4change.application.port.TopicBadgeStatsDto
import com.ureka.play4change.application.port.UserBadgeDto
import com.ureka.play4change.domain.badge.BadgeRepository
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.topic.TopicRepository
import org.springframework.stereotype.Service
import kotlin.math.roundToInt

@Service
class BadgeQueryService(
    private val badgeRepository: BadgeRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val topicRepository: TopicRepository
) : BadgeQueryUseCase {

    override fun getUserBadges(userId: String): List<UserBadgeDto> =
        badgeRepository.findBadgesByUserId(userId).mapNotNull { badge ->
            val competence = badgeRepository.findMicroCompetenceById(badge.microCompetenceId)
                ?: return@mapNotNull null
            val topic = topicRepository.findById(competence.topicId)
                ?: return@mapNotNull null
            UserBadgeDto(
                microCompetenceName = competence.name,
                description = competence.description,
                topicTitle = topic.title,
                earnedAt = badge.earnedAt
            )
        }

    override fun getTopicBadgeStats(topicId: String): TopicBadgeStatsDto {
        val competence = badgeRepository.findMicroCompetenceByTopicId(topicId)
            ?: return TopicBadgeStatsDto(0, 0, 0.0, emptyList())
        val badges = badgeRepository.findBadgesByMicroCompetenceId(competence.id)
        val enrolledCount = enrollmentRepository.countByTopicId(topicId).toInt()
        val earnedPercentage = if (enrolledCount == 0) 0.0
            else ((badges.size.toDouble() / enrolledCount) * PERCENT_FACTOR * ROUNDING_FACTOR)
                .roundToInt() / ROUNDING_FACTOR
        return TopicBadgeStatsDto(
            totalIssued = badges.size,
            enrolledCount = enrolledCount,
            earnedPercentage = earnedPercentage,
            recentEarners = badges
                .sortedByDescending { it.earnedAt }
                .take(RECENT_EARNERS_LIMIT)
                .map { RecentEarnerDto(it.userId, it.earnedAt) }
        )
    }

    companion object {
        private const val PERCENT_FACTOR = 100.0
        private const val ROUNDING_FACTOR = 100.0
        private const val RECENT_EARNERS_LIMIT = 10
    }
}
