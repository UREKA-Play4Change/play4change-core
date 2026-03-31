package com.ureka.play4change.domain.topic

interface TopicRepository {
    fun findById(id: String): Topic?
    fun findAllActive(): List<Topic>
    fun save(topic: Topic): Topic
    fun updateStatus(id: String, status: TopicStatus)
}
