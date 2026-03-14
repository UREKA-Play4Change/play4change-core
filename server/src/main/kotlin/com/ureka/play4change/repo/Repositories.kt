package com.ureka.play4change.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CourseRepository : JpaRepository<CourseEntity, String>

@Repository
interface CourseModuleRepository : JpaRepository<CourseModuleEntity, String>

@Repository
interface TaskTemplateRepository : JpaRepository<TaskTemplateEntity, String> {
    fun findByModuleIdAndDayIndex(moduleId: String, dayIndex: Int): List<TaskTemplateEntity>
}

@Repository
interface UserTaskRepository : JpaRepository<UserTaskEntity, String> {

    @Query("""
        SELECT ut FROM UserTaskEntity ut
        WHERE ut.userId = :userId
          AND ut.taskTemplate.module.id = :moduleId
          AND ut.taskTemplate.dayIndex = :dayIndex
    """)
    fun findExistingAssignment(
        @Param("userId") userId: String,
        @Param("moduleId") moduleId: String,
        @Param("dayIndex") dayIndex: Int
    ): UserTaskEntity?
}