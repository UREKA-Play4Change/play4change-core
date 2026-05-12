package com.ureka.play4change.application.badge

import com.ureka.play4change.application.port.BadgeIssuancePort
import com.ureka.play4change.domain.badge.Badge
import com.ureka.play4change.domain.badge.BadgeRepository
import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.domain.topic.TopicRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class BadgeIssuanceService(
    private val badgeRepository: BadgeRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val topicRepository: TopicRepository,
    private val taskTemplateRepository: TaskTemplateRepository,
    private val topicModuleRepository: TopicModuleRepository
) : BadgeIssuancePort {

    private val log = LoggerFactory.getLogger(BadgeIssuanceService::class.java)

    override fun issueBadge(userId: String, topicId: String, enrollmentId: String) {
        topicRepository.findById(topicId) ?: run {
            log.warn("Badge issuance skipped — topic {} not found", topicId)
            return
        }
        val module = topicModuleRepository.findByTopicId(topicId).firstOrNull() ?: run {
            log.warn("Badge issuance skipped — no module for topic {}", topicId)
            return
        }
        val actualTemplateCount = taskTemplateRepository.findCurrentByModuleId(module.id).size
        if (actualTemplateCount == 0) {
            log.warn("Badge issuance skipped — no templates in topic {}", topicId)
            return
        }
        val assignments = enrollmentRepository.findAssignmentsByEnrollmentId(enrollmentId)
        val correctCount = assignments.count { it.isCorrect == true && it.status != AssignmentStatus.PENDING }
        if (correctCount >= actualTemplateCount) {
            issueIfNotYetEarned(userId, topicId)
        } else {
            log.debug(
                "Badge not yet earned for user {} in topic {} — {}/{} tasks correct",
                userId, topicId, correctCount, actualTemplateCount
            )
        }
    }

    private fun issueIfNotYetEarned(userId: String, topicId: String) {
        val microCompetence = badgeRepository.findMicroCompetenceByTopicId(topicId) ?: return
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
