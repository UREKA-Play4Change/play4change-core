package com.ureka.play4change.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "topic_phase_log")
class TopicPhaseLogEntity(

    @Id
    val id: String,

    @Column(name = "topic_id", nullable = false, length = 36)
    val topicId: String,

    @Column(name = "from_phase", nullable = false, length = 20)
    val fromPhase: String,

    @Column(name = "to_phase", nullable = false, length = 20)
    val toPhase: String,

    @Column(name = "transitioned_at", nullable = false)
    val transitionedAt: OffsetDateTime,

    @Column(name = "duration_ms", nullable = false)
    val durationMs: Long
)
