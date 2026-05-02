package com.ureka.play4change.infrastructure.persistence.adapter

import com.ureka.play4change.domain.topic.GenerationPhase
import com.ureka.play4change.domain.topic.TopicPhaseLog
import com.ureka.play4change.domain.topic.TopicPhaseLogRepository
import com.ureka.play4change.infrastructure.persistence.entity.TopicPhaseLogEntity
import com.ureka.play4change.infrastructure.persistence.repository.TopicPhaseLogJpaRepository
import org.springframework.stereotype.Component

@Component
class TopicPhaseLogRepositoryAdapter(
    private val jpa: TopicPhaseLogJpaRepository
) : TopicPhaseLogRepository {

    override fun save(log: TopicPhaseLog): TopicPhaseLog =
        jpa.save(log.toEntity()).toDomain()

    override fun findByTopicId(topicId: String): List<TopicPhaseLog> =
        jpa.findByTopicIdOrderByTransitionedAtAsc(topicId).map { it.toDomain() }

    private fun TopicPhaseLog.toEntity() = TopicPhaseLogEntity(
        id = id,
        topicId = topicId,
        fromPhase = fromPhase.name,
        toPhase = toPhase.name,
        transitionedAt = transitionedAt,
        durationMs = durationMs
    )

    private fun TopicPhaseLogEntity.toDomain() = TopicPhaseLog(
        id = id,
        topicId = topicId,
        fromPhase = GenerationPhase.valueOf(fromPhase),
        toPhase = GenerationPhase.valueOf(toPhase),
        transitionedAt = transitionedAt,
        durationMs = durationMs
    )
}
