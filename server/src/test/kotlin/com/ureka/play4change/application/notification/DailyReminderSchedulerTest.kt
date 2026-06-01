package com.ureka.play4change.application.notification

import com.ureka.play4change.application.port.PushNotificationPort
import com.ureka.play4change.domain.enrollment.AssignmentStatus
import com.ureka.play4change.domain.enrollment.Enrollment
import com.ureka.play4change.domain.enrollment.EnrollmentRepository
import com.ureka.play4change.domain.enrollment.EnrollmentStatus
import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.domain.identity.AuthProvider
import com.ureka.play4change.domain.identity.User
import com.ureka.play4change.domain.identity.UserRepository
import com.ureka.play4change.domain.identity.UserRole
import com.ureka.play4change.domain.notification.DeviceToken
import com.ureka.play4change.domain.notification.DeviceTokenPlatform
import com.ureka.play4change.domain.notification.DeviceTokenRepository
import com.ureka.play4change.domain.topic.TaskType
import com.ureka.play4change.domain.topic.TopicRepository
import com.ureka.play4change.infrastructure.scheduler.DailyReminderScheduler
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyReminderSchedulerTest {

    private val deviceTokenRepository = mockk<DeviceTokenRepository>()
    private val enrollmentRepository = mockk<EnrollmentRepository>()
    private val userRepository = mockk<UserRepository>()
    private val topicRepository = mockk<TopicRepository>()
    private val pushNotificationPort = mockk<PushNotificationPort>()

    // Fixed clock: 2024-01-01 20:00:00 UTC
    private val fixedUtc20h: ZonedDateTime = ZonedDateTime.of(2024, 1, 1, 20, 0, 0, 0, ZoneId.of("UTC"))

    private lateinit var scheduler: DailyReminderScheduler

    @BeforeEach
    fun setUp() {
        scheduler = object : DailyReminderScheduler(
            deviceTokenRepository,
            enrollmentRepository,
            userRepository,
            topicRepository,
            pushNotificationPort
        ) {
            override fun nowInZone(zone: ZoneId): ZonedDateTime =
                fixedUtc20h.withZoneSameInstant(zone)
        }
    }

    @Test
    fun `user with incomplete task at 20h local time receives notification`() {
        val userId = "u1"
        val token = aToken(userId)
        val user = aUser(userId, timezone = "UTC")
        val enrollment = anEnrollment(userId)

        every { deviceTokenRepository.findAll() } returns listOf(token)
        every { userRepository.findById(userId) } returns user
        every { enrollmentRepository.findActiveByUserId(userId) } returns listOf(enrollment)
        every { enrollmentRepository.findAssignmentsByEnrollmentId(enrollment.id) } returns emptyList()
        every { topicRepository.findById(enrollment.topicId) } returns null
        justRun { pushNotificationPort.send(any(), any(), any()) }
        justRun { deviceTokenRepository.updateLastNotifiedAt(any(), any()) }

        scheduler.sendDailyReminders()

        verify(exactly = 1) { pushNotificationPort.send(token, any(), any()) }
    }

    @Test
    fun `user who already submitted today does not receive notification`() {
        val userId = "u2"
        val token = aToken(userId)
        val user = aUser(userId, timezone = "UTC")
        val enrollment = anEnrollment(userId)
        val submitted = anAssignment(
            enrollmentId = enrollment.id,
            submittedAt = OffsetDateTime.parse("2024-01-01T15:00:00Z"),
            status = AssignmentStatus.SUBMITTED
        )

        every { deviceTokenRepository.findAll() } returns listOf(token)
        every { userRepository.findById(userId) } returns user
        every { enrollmentRepository.findActiveByUserId(userId) } returns listOf(enrollment)
        every { enrollmentRepository.findAssignmentsByEnrollmentId(enrollment.id) } returns listOf(submitted)

        scheduler.sendDailyReminders()

        verify(exactly = 0) { pushNotificationPort.send(any(), any(), any()) }
    }

    @Test
    fun `user with no device token is skipped`() {
        every { deviceTokenRepository.findAll() } returns emptyList()

        scheduler.sendDailyReminders()

        verify(exactly = 0) { pushNotificationPort.send(any(), any(), any()) }
    }

    @Test
    fun `user whose local time is not 20h does not receive notification`() {
        val userId = "u3"
        // User is in UTC+3 — 20:00 UTC = 23:00 local — not the reminder hour
        val token = aToken(userId)
        val user = aUser(userId, timezone = "Africa/Nairobi") // UTC+3

        every { deviceTokenRepository.findAll() } returns listOf(token)
        every { userRepository.findById(userId) } returns user

        scheduler.sendDailyReminders()

        verify(exactly = 0) { pushNotificationPort.send(any(), any(), any()) }
    }

    @Test
    fun `user already notified today is not notified again`() {
        val userId = "u4"
        // lastNotifiedAt = same date in UTC, so already notified today
        val notifiedToday = OffsetDateTime.parse("2024-01-01T10:00:00Z")
        val token = aToken(userId, lastNotifiedAt = notifiedToday)
        val user = aUser(userId, timezone = "UTC")

        every { deviceTokenRepository.findAll() } returns listOf(token)
        every { userRepository.findById(userId) } returns user

        scheduler.sendDailyReminders()

        verify(exactly = 0) { pushNotificationPort.send(any(), any(), any()) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun aToken(
        userId: String,
        lastNotifiedAt: OffsetDateTime? = null
    ) = DeviceToken(
        id = "dt-$userId",
        userId = userId,
        token = "fcm-token-$userId",
        platform = DeviceTokenPlatform.ANDROID,
        lastNotifiedAt = lastNotifiedAt,
        createdAt = OffsetDateTime.now(),
        updatedAt = OffsetDateTime.now()
    )

    private fun aUser(userId: String, timezone: String) = User(
        id = userId,
        email = "$userId@test.com",
        name = null,
        avatarUrl = null,
        role = UserRole.USER,
        provider = AuthProvider.MAGIC_LINK,
        providerId = null,
        preferredLanguage = "en",
        audienceLevel = "BEGINNER",
        createdAt = OffsetDateTime.now(),
        timezone = timezone
    )

    private fun anEnrollment(userId: String) = Enrollment(
        id = "enr-$userId",
        userId = userId,
        topicId = "topic-1",
        topicModuleId = "mod-1",
        enrolledAt = OffsetDateTime.now().minusDays(3),
        status = EnrollmentStatus.ACTIVE,
        currentDayIndex = 2,
        totalPointsEarned = 20,
        streakDays = 2,
        lastActivityAt = null
    )

    private fun anAssignment(
        enrollmentId: String,
        submittedAt: OffsetDateTime?,
        status: AssignmentStatus
    ) = TaskAssignment(
        id = "asn-1",
        enrollmentId = enrollmentId,
        userId = "any",
        taskTemplateId = "tmpl-1",
        taskTemplateVersion = 1,
        taskType = TaskType.MULTIPLE_CHOICE,
        assignedAt = OffsetDateTime.now().minusDays(1),
        dueAt = OffsetDateTime.now().plusDays(1),
        submittedAt = submittedAt,
        status = status,
        selectedOption = 0,
        isCorrect = true,
        pointsAwarded = 10,
        optionOrder = listOf(0, 1, 2, 3),
        wrongAttemptCount = 0,
        photoUrl = null
    )
}
