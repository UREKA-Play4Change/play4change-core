package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.TaskAssignmentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TaskAssignmentJpaRepository : JpaRepository<TaskAssignmentEntity, String> {

    fun findByEnrollmentIdAndTaskTemplateId(
        enrollmentId: String,
        taskTemplateId: String
    ): TaskAssignmentEntity?

    fun findByEnrollmentIdAndStatus(enrollmentId: String, status: String): List<TaskAssignmentEntity>

    @Query("""
        SELECT a FROM TaskAssignmentEntity a
        WHERE a.enrollment.id = :enrollmentId
          AND a.taskTemplate.dayIndex = :dayIndex
        ORDER BY a.assignedAt DESC
    """)
    fun findByEnrollmentIdAndDayIndex(
        @Param("enrollmentId") enrollmentId: String,
        @Param("dayIndex") dayIndex: Int
    ): List<TaskAssignmentEntity>

    fun findByUserIdAndStatus(userId: String, status: String): List<TaskAssignmentEntity>

    fun findAllByEnrollmentId(enrollmentId: String): List<TaskAssignmentEntity>

    @Query("""
        SELECT a FROM TaskAssignmentEntity a
        WHERE a.enrollment.topic.id = :topicId
          AND a.taskType = :taskType
          AND a.status = :status
          AND a.userId != :excludeUserId
    """)
    fun findByTopicAndTypeAndStatusExcludingUser(
        @Param("topicId") topicId: String,
        @Param("taskType") taskType: String,
        @Param("status") status: String,
        @Param("excludeUserId") excludeUserId: String
    ): List<TaskAssignmentEntity>
}
