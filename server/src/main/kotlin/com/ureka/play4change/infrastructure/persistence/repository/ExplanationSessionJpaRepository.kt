package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.ExplanationSessionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ExplanationSessionJpaRepository : JpaRepository<ExplanationSessionEntity, String> {

    @Query(
        """
        SELECT e FROM ExplanationSessionEntity e
        WHERE e.enrollment.id = :enrollmentId
          AND e.status IN ('GENERATING', 'ACTIVE')
        ORDER BY e.generatedAt DESC
        """
    )
    fun findActiveByEnrollmentId(enrollmentId: String): ExplanationSessionEntity?

    fun findByEnrollmentIdOrderByGeneratedAtAsc(enrollmentId: String): List<ExplanationSessionEntity>
}
