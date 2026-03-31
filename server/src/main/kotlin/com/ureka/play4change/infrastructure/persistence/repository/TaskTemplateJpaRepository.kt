package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.TaskTemplateEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TaskTemplateJpaRepository : JpaRepository<TaskTemplateEntity, String> {

    fun findByModuleIdAndDayIndexAndIsCurrentTrue(moduleId: String, dayIndex: Int): List<TaskTemplateEntity>

    fun findByModuleIdAndDayIndexAndPoolIndexAndIsCurrentTrue(
        moduleId: String,
        dayIndex: Int,
        poolIndex: Int
    ): TaskTemplateEntity?

    @Query("""
        SELECT t FROM TaskTemplateEntity t
        WHERE t.module.id = :moduleId
          AND t.isCurrent = TRUE
        ORDER BY t.dayIndex ASC, t.poolIndex ASC
    """)
    fun findCurrentByModuleId(@Param("moduleId") moduleId: String): List<TaskTemplateEntity>
}
