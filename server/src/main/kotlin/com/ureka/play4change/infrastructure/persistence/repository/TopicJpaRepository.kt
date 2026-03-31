package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.TopicEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TopicJpaRepository : JpaRepository<TopicEntity, String> {
    fun findAllByStatus(status: String): List<TopicEntity>
    fun findAllByCreatedBy(createdBy: String): List<TopicEntity>
}
