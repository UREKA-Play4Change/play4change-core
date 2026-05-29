package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.TopicPrerequisiteEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TopicPrerequisiteJpaRepository : JpaRepository<TopicPrerequisiteEntity, String> {
    fun findAllByTopicId(topicId: String): List<TopicPrerequisiteEntity>
    fun deleteAllByTopicId(topicId: String)
}
