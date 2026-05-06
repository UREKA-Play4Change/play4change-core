package com.ureka.play4change.infrastructure.persistence.repository

import com.ureka.play4change.infrastructure.persistence.entity.TaskInstanceEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface TaskInstanceJpaRepository : JpaRepository<TaskInstanceEntity, String> {

    @Query("SELECT t FROM TaskInstanceEntity t WHERE t.taskTemplate.id = :taskTemplateId ORDER BY t.instanceIndex ASC")
    fun findByTaskTemplateIdOrderByInstanceIndex(taskTemplateId: String): List<TaskInstanceEntity>

    @Modifying
    @Transactional
    @Query("DELETE FROM TaskInstanceEntity t WHERE t.taskTemplate.id = :taskTemplateId")
    fun deleteByTaskTemplateId(taskTemplateId: String)
}
