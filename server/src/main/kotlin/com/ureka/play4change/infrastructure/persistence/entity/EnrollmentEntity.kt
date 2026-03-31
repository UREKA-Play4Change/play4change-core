package com.ureka.play4change.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "enrollments")
class EnrollmentEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "user_id", nullable = false, length = 36)
    val userId: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    val topic: TopicEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_module_id", nullable = false)
    val topicModule: TopicModuleEntity,

    @Column(name = "enrolled_at", nullable = false)
    val enrolledAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(nullable = false, length = 15)
    var status: String = "ACTIVE",

    @Column(name = "current_day_index", nullable = false)
    var currentDayIndex: Int = 0,

    @Column(name = "total_points_earned", nullable = false)
    var totalPointsEarned: Int = 0,

    @Column(name = "streak_days", nullable = false)
    var streakDays: Int = 0,

    @Column(name = "last_activity_at")
    var lastActivityAt: OffsetDateTime? = null
)
