package com.ureka.play4change.domain.enrollment

import java.time.OffsetDateTime

data class Enrollment(
    val id: String,
    val userId: String,
    val topicId: String,
    val topicModuleId: String,
    val enrolledAt: OffsetDateTime,
    val status: EnrollmentStatus,
    val currentDayIndex: Int,
    val totalPointsEarned: Int,
    val streakDays: Int,
    val lastActivityAt: OffsetDateTime?
) {
    fun advanceDay(): Enrollment =
        copy(
            currentDayIndex = currentDayIndex + 1,
            lastActivityAt = OffsetDateTime.now()
        )

    fun addPoints(points: Int): Enrollment =
        copy(
            totalPointsEarned = totalPointsEarned + points,
            lastActivityAt = OffsetDateTime.now()
        )

    fun incrementStreak(): Enrollment =
        copy(
            streakDays = streakDays + 1,
            lastActivityAt = OffsetDateTime.now()
        )

    fun resetStreak(): Enrollment =
        copy(streakDays = 0)
}
