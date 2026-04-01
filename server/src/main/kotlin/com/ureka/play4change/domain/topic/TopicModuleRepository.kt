package com.ureka.play4change.domain.topic

interface TopicModuleRepository {
    fun save(module: TopicModule): TopicModule
    fun findByTopicId(topicId: String): List<TopicModule>
    fun deleteByTopicId(topicId: String)
}
