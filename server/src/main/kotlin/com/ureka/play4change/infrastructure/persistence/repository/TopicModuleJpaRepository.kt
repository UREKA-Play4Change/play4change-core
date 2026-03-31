package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.TopicModuleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TopicModuleJpaRepository : JpaRepository<TopicModuleEntity, String> {
    fun findByTopicIdOrderByOrderIndexAsc(topicId: String): List<TopicModuleEntity>
}
