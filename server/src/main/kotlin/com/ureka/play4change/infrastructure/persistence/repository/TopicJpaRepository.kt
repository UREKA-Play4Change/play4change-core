package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.TopicEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
interface TopicJpaRepository : JpaRepository<TopicEntity, String> {
    fun findAllByStatus(status: String): List<TopicEntity>
    fun findAllByStatus(status: String, pageable: Pageable): Page<TopicEntity>
    fun findAllByCreatedBy(createdBy: String): List<TopicEntity>
    fun countByStatus(status: String): Long
    fun findAllByStatusAndExpiresAtBefore(status: String, now: OffsetDateTime): List<TopicEntity>
    fun findAllByStatusAndStatusUpdatedAtBefore(status: String, cutoff: OffsetDateTime): List<TopicEntity>
}
