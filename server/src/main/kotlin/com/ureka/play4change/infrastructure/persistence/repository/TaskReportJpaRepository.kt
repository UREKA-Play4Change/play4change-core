package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.TaskReportEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskReportJpaRepository : JpaRepository<TaskReportEntity, String> {
    fun findByUserIdAndTaskTemplateId(userId: String, taskTemplateId: String): TaskReportEntity?
    fun findByStatus(status: String, pageable: Pageable): Page<TaskReportEntity>
}
