package com.ureka.play4change.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "adaptive_tasks")
class AdaptiveTaskEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "struggle_session_id", nullable = false)
    val struggleSession: StruggleSessionEntity,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(columnDefinition = "TEXT")
    val hint: String? = null,

    @Column(name = "points_reward", nullable = false)
    val pointsReward: Int = 10,

    @Column(name = "order_index", nullable = false)
    val orderIndex: Int,

    @Column(name = "completed_at")
    var completedAt: OffsetDateTime? = null,

    @Column(name = "is_correct")
    var isCorrect: Boolean? = null
)
