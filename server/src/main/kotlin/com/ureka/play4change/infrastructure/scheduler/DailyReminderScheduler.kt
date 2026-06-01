package com.ureka.play4change.infrastructure.scheduler

import com.ureka.play4change.application.port.PushNotificationPort
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.domain.notification.DeviceToken
import com.ureka.play4change.domain.notification.DeviceTokenRepository
import com.ureka.play4change.domain.topic.TopicRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Runs every hour on the hour.
 * For each registered device token, checks whether the current hour in the user's
 * local timezone is 20 (8 PM) and today's task is incomplete; if so, sends a push
 * notification and records the send time in [DeviceToken.lastNotifiedAt] to avoid duplicates.
 *
 * [nowInZone] is protected-open so unit tests can supply a fixed clock.
 */
@Component
open class DailyReminderScheduler(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val userRepository: UserRepository,
    private val topicRepository: TopicRepository,
    private val pushNotificationPort: PushNotificationPort
) {

    private val logger = LoggerFactory.getLogger(DailyReminderScheduler::class.java)

    @Scheduled(cron = "0 0 * * * *")
    fun sendDailyReminders() {
        val tokens = deviceTokenRepository.findAll()
        logger.info("Daily reminder check: {} device token(s)", tokens.size)
        for (token in tokens) {
            runCatching { checkAndNotify(token) }
                .onFailure { logger.error("Reminder processing failed for token {}: {}", token.id, it.message) }
        }
    }

    internal open fun nowInZone(zone: ZoneId): ZonedDateTime = ZonedDateTime.now(zone)

    private fun checkAndNotify(deviceToken: DeviceToken) {
        val user = userRepository.findById(deviceToken.userId) ?: return
        val tz = runCatching { ZoneId.of(user.timezone ?: "UTC") }.getOrDefault(ZoneId.of("UTC"))
        val nowInTz = nowInZone(tz)

        if (nowInTz.hour != REMINDER_HOUR) return

        val todayInTz = nowInTz.toLocalDate()
        if (deviceToken.lastNotifiedAt?.atZoneSameInstant(tz)?.toLocalDate() == todayInTz) return

        val enrollments = enrollmentRepository.findActiveByUserId(deviceToken.userId)
        if (enrollments.isEmpty()) return

        val hasSubmittedToday = enrollments.any { enrollment ->
            enrollmentRepository.findAssignmentsByEnrollmentId(enrollment.id).any { assignment ->
                assignment.submittedAt?.atZoneSameInstant(tz)?.toLocalDate() == todayInTz
            }
        }
        if (hasSubmittedToday) return

        val topicName = topicRepository.findById(enrollments.first().topicId)?.title ?: "your topic"
        pushNotificationPort.send(
            deviceToken,
            "Your daily challenge is waiting \uD83C\uDFAF",
            "Complete today's task in $topicName to keep your streak."
        )
        deviceTokenRepository.updateLastNotifiedAt(deviceToken.id, OffsetDateTime.now())
        logger.info("Daily reminder sent to user {}", deviceToken.userId)
    }

    private companion object {
        const val REMINDER_HOUR = 20
    }
}
