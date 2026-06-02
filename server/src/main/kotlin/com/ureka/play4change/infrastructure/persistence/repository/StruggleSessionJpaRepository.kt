package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.AdaptiveTaskEntity
import com.ureka.play4change.infrastructure.persistence.entity.StruggleSessionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface StruggleSessionJpaRepository : JpaRepository<StruggleSessionEntity, String> {
    fun findFirstByEnrollmentIdAndStatusOrderByDetectedAtDesc(enrollmentId: String, status: String): StruggleSessionEntity?

    @Query("""
        SELECT at FROM AdaptiveTaskEntity at
        WHERE at.struggleSession.enrollment.topic.id = :topicId
        ORDER BY at.struggleSession.detectedAt DESC, at.orderIndex ASC
    """)
    fun findAdaptiveTasksByTopicId(@Param("topicId") topicId: String): List<AdaptiveTaskEntity>
}
