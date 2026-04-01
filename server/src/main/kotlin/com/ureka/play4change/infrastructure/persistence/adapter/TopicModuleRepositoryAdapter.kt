package com.ureka.play4change.infrastructure.persistence.adapter

import com.ureka.play4change.domain.topic.TopicModule
import com.ureka.play4change.domain.topic.TopicModuleRepository
import com.ureka.play4change.infrastructure.persistence.entity.TopicModuleEntity
import com.ureka.play4change.infrastructure.persistence.repository.TopicJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.TopicModuleJpaRepository
import org.springframework.stereotype.Component

@Component
class TopicModuleRepositoryAdapter(
    private val jpa: TopicModuleJpaRepository,
    private val topicJpa: TopicJpaRepository
) : TopicModuleRepository {

    override fun save(module: TopicModule): TopicModule {
        val topicRef = topicJpa.getReferenceById(module.topicId)
        return jpa.save(module.toEntity(topicRef)).toDomain()
    }

    override fun findByTopicId(topicId: String): List<TopicModule> =
        jpa.findByTopicIdOrderByOrderIndexAsc(topicId).map { it.toDomain() }

    override fun deleteByTopicId(topicId: String) {
        val modules = jpa.findByTopicIdOrderByOrderIndexAsc(topicId)
        jpa.deleteAll(modules)
    }

    private fun TopicModuleEntity.toDomain() = TopicModule(
        id = id,
        topicId = topic.id,
        orderIndex = orderIndex,
        objective = objective
    )

    private fun TopicModule.toEntity(topicRef: com.ureka.play4change.infrastructure.persistence.entity.TopicEntity) =
        TopicModuleEntity(
            id = id,
            topic = topicRef,
            orderIndex = orderIndex,
            objective = objective
        )
}
