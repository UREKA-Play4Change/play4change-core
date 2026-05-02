package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.TopicPhaseLogEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TopicPhaseLogJpaRepository : JpaRepository<TopicPhaseLogEntity, String> {
    fun findByTopicIdOrderByTransitionedAtAsc(topicId: String): List<TopicPhaseLogEntity>
}
