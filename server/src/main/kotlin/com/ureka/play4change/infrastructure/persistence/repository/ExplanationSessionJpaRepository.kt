package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.ExplanationSessionEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

    @Query("SELECT e FROM ExplanationSessionEntity e WHERE e.enrollment.id = :enrollmentId ORDER BY e.generatedAt ASC")
    fun findByEnrollmentIdPaged(enrollmentId: String, pageable: Pageable): Page<ExplanationSessionEntity>

    @Query(
        """
        SELECT e FROM ExplanationSessionEntity e
        JOIN FETCH e.enrollment enr
        JOIN FETCH e.originalTaskAssignment ta
        JOIN FETCH ta.taskTemplate tt
        WHERE enr.topic.id = :topicId
        ORDER BY e.generatedAt DESC
        """
    )
    fun findByTopicIdWithDetails(topicId: String): List<ExplanationSessionEntity>

    @Query(
        value = """
            SELECT e FROM ExplanationSessionEntity e
            JOIN FETCH e.enrollment enr
            JOIN FETCH e.originalTaskAssignment ta
            JOIN FETCH ta.taskTemplate tt
            WHERE enr.topic.id = :topicId
            ORDER BY e.generatedAt DESC
        """,
        countQuery = "SELECT COUNT(e) FROM ExplanationSessionEntity e WHERE e.enrollment.topic.id = :topicId"
    )
    fun findByTopicIdWithDetails(topicId: String, pageable: Pageable): Page<ExplanationSessionEntity>
}
