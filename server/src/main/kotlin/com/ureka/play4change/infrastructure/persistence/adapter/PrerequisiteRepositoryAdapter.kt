package com.ureka.play4change.infrastructure.persistence.adapter

import com.ureka.play4change.domain.topic.PrerequisiteRepository
import com.ureka.play4change.infrastructure.persistence.entity.TopicPrerequisiteEntity
import com.ureka.play4change.infrastructure.persistence.repository.TopicPrerequisiteJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PrerequisiteRepositoryAdapter(
    private val jpa: TopicPrerequisiteJpaRepository
) : PrerequisiteRepository {

    override fun findPrerequisitesByTopicId(topicId: String): List<String> =
        jpa.findAllByTopicId(topicId).map { it.prerequisiteTopicId }

    @Transactional
    override fun setPrerequisites(topicId: String, prerequisiteIds: List<String>) {
        jpa.deleteAllByTopicId(topicId)
        jpa.flush()
        prerequisiteIds.forEach { prereqId ->
            jpa.save(TopicPrerequisiteEntity(topicId = topicId, prerequisiteTopicId = prereqId))
        }
    }

    override fun findAllEdges(): List<Pair<String, String>> =
        jpa.findAll().map { it.topicId to it.prerequisiteTopicId }
}
