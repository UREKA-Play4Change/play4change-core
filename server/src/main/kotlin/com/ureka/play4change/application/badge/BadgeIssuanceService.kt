package com.ureka.play4change.application.badge

import com.ureka.play4change.application.port.BadgeIssuancePort
import com.ureka.play4change.domain.badge.Badge
import com.ureka.play4change.domain.badge.BadgeRepository
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.topic.TopicRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

private const val MINIMUM_CORRECT_RATE = 0.60

@Service
class BadgeIssuanceService(
    private val badgeRepository: BadgeRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val topicRepository: TopicRepository
) : BadgeIssuancePort {

    private val log = LoggerFactory.getLogger(BadgeIssuanceService::class.java)

    override fun issueBadge(userId: String, topicId: String, enrollmentId: String) {
        val topic = topicRepository.findById(topicId) ?: run {
            log.warn("Badge issuance skipped — topic {} not found", topicId)
            return
        }
        val submittedCount = enrollmentRepository.countSubmittedAssignmentsByEnrollmentId(enrollmentId)
        val correctCount = enrollmentRepository.countCorrectAssignmentsByEnrollmentId(enrollmentId)
        val requiredCorrect = (topic.taskCount * MINIMUM_CORRECT_RATE).toInt()
        if (submittedCount >= topic.taskCount && correctCount >= requiredCorrect) {
            issueIfNotYetEarned(userId, topicId)
        } else {
            log.debug(
                "Badge not yet earned for user {} in topic {} — {}/{} submitted, {}/{} correct",
                userId, topicId, submittedCount, topic.taskCount, correctCount, requiredCorrect
            )
        }
    }

    private fun issueIfNotYetEarned(userId: String, topicId: String) {
        val microCompetence = badgeRepository.findMicroCompetenceByTopicId(topicId) ?: run {
            log.warn("Badge issuance skipped — no MicroCompetence found for topic {} (was INDEXING phase run for this topic?)", topicId)
            return
        }
        if (badgeRepository.findBadgeByUserIdAndMicroCompetenceId(userId, microCompetence.id) != null) return
        badgeRepository.saveBadge(
            Badge(
                id = UUID.randomUUID().toString(),
                userId = userId,
                microCompetenceId = microCompetence.id,
                earnedAt = OffsetDateTime.now()
            )
        )
        log.info("Badge issued for user {} in topic {} (competence {})", userId, topicId, microCompetence.id)
    }
}
