package com.ureka.play4change.domain.topic

interface TopicPhaseLogRepository {
    fun save(log: TopicPhaseLog): TopicPhaseLog
    fun findByTopicId(topicId: String): List<TopicPhaseLog>
}
