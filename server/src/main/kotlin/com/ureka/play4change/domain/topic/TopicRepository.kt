package com.ureka.play4change.domain.topic

import java.time.OffsetDateTime

interface TopicRepository {
    fun findById(id: String): Topic?
    fun findAllActive(): List<Topic>
    fun findAll(): List<Topic>
    fun findAll(page: Int, size: Int): PageResult<Topic>
    fun findByStatus(status: TopicStatus): List<Topic>
    fun findByStatus(status: TopicStatus, page: Int, size: Int): PageResult<Topic>
    fun findActiveExpired(): List<Topic>
    fun findStuckGenerating(cutoff: OffsetDateTime): List<Topic>
    fun save(topic: Topic): Topic
    fun updateStatus(id: String, status: TopicStatus)
}
