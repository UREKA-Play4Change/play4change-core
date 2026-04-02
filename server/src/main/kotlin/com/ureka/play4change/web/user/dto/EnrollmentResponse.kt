package com.ureka.play4change.web.user.dto

import com.ureka.play4change.domain.enrollment.Enrollment
import java.time.OffsetDateTime

data class EnrollmentResponse(
    val id: String,
    val topicId: String,
    val status: String,
    val currentDayIndex: Int,
    val totalPointsEarned: Int,
    val streakDays: Int,
    val enrolledAt: OffsetDateTime
) {
    companion object {
        fun from(e: Enrollment) = EnrollmentResponse(
            id = e.id,
            topicId = e.topicId,
            status = e.status.name,
            currentDayIndex = e.currentDayIndex,
            totalPointsEarned = e.totalPointsEarned,
            streakDays = e.streakDays,
            enrolledAt = e.enrolledAt
        )
    }
}
