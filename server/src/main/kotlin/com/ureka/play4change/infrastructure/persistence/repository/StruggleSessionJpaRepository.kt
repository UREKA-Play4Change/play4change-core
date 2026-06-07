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
    fun findByEnrollmentIdOrderByDetectedAtAsc(enrollmentId: String): List<StruggleSessionEntity>
    fun countByEnrollmentIdAndOriginalTaskAssignmentId(enrollmentId: String, originalTaskAssignmentId: String): Int

    @Query("""
        SELECT at FROM AdaptiveTaskEntity at
        WHERE at.struggleSession.enrollment.topic.id = :topicId
        AND at.branchId IS NOT NULL
        ORDER BY at.struggleSession.detectedAt DESC, at.orderIndex ASC
    """)
    fun findAdaptiveTasksByTopicId(@Param("topicId") topicId: String): List<AdaptiveTaskEntity>

    @Query(value = """
        SELECT ta.task_template_id AS taskTemplateId, ss.error_pattern AS errorPattern,
               CAST(COUNT(ss.id) AS int) AS totalSessions
        FROM struggle_sessions ss
        JOIN task_assignments ta ON ta.id = ss.original_task_assignment_id
        JOIN enrollments e ON e.id = ss.enrollment_id
        WHERE e.topic_id = :topicId
        GROUP BY ta.task_template_id, ss.error_pattern
    """, nativeQuery = true)
    fun findPathStatsByTopicId(@Param("topicId") topicId: String): List<StrugglePathStatsRow>

    interface StrugglePathStatsRow {
        fun getTaskTemplateId(): String
        fun getErrorPattern(): String
        fun getTotalSessions(): Int
    }

    @Query("""
        SELECT DISTINCT at.branchId FROM AdaptiveTaskEntity at
        WHERE at.struggleSession.enrollment.id = :enrollmentId
        AND at.struggleSession.originalTaskAssignment.id = :assignmentId
        AND at.branchId IS NOT NULL
    """)
    fun findUsedBranchIds(
        @Param("enrollmentId") enrollmentId: String,
        @Param("assignmentId") assignmentId: String
    ): List<String>
}
