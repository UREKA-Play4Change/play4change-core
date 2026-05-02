package com.ureka.play4change.domain.topic

import java.time.OffsetDateTime

data class TopicPhaseLog(
    val id: String,
    val topicId: String,
    val fromPhase: GenerationPhase,
    val toPhase: GenerationPhase,
    val transitionedAt: OffsetDateTime,
    val durationMs: Long
)
