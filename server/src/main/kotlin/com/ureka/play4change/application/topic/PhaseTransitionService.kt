package com.ureka.play4change.application.topic

import com.ureka.play4change.domain.topic.GenerationPhase
import com.ureka.play4change.domain.topic.TopicPhaseLog
import com.ureka.play4change.domain.topic.TopicPhaseLogRepository
import com.ureka.play4change.domain.topic.TopicRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Records a phase transition for a topic.
 *
 * Reads the topic's current phase and phaseUpdatedAt, calculates the duration spent in
 * the previous phase, persists a [TopicPhaseLog] entry, then updates the topic's
 * currentPhase and phaseUpdatedAt.
 *
 * Phase transitions happen here, in the application layer — not in infrastructure adapters.
 */
@Service
class PhaseTransitionService(
    private val topicRepository: TopicRepository,
    private val phaseLogRepository: TopicPhaseLogRepository
) {
    private val log = LoggerFactory.getLogger(PhaseTransitionService::class.java)

    fun transitionTo(topicId: String, toPhase: GenerationPhase) {
        val topic = topicRepository.findById(topicId) ?: run {
            log.warn("Phase transition to {} skipped — topic {} not found", toPhase, topicId)
            return
        }
        val now = OffsetDateTime.now()
        val durationMs = Duration.between(topic.phaseUpdatedAt.toInstant(), now.toInstant()).toMillis()

        phaseLogRepository.save(
            TopicPhaseLog(
                id = UUID.randomUUID().toString(),
                topicId = topicId,
                fromPhase = topic.currentPhase,
                toPhase = toPhase,
                transitionedAt = now,
                durationMs = durationMs
            )
        )
        topicRepository.updatePhase(topicId, toPhase, now)
        log.info("Topic {} phase: {} → {} ({}ms)", topicId, topic.currentPhase, toPhase, durationMs)
    }
}
