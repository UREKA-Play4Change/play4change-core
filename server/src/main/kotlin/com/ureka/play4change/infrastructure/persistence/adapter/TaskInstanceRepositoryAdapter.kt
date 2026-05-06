package com.ureka.play4change.infrastructure.persistence.adapter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ureka.play4change.domain.topic.TaskInstance
import com.ureka.play4change.domain.topic.TaskInstanceRepository
import com.ureka.play4change.infrastructure.persistence.entity.TaskInstanceEntity
import com.ureka.play4change.infrastructure.persistence.repository.TaskAssignmentJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.TaskInstanceJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.TaskTemplateJpaRepository
import org.springframework.stereotype.Component

@Component
class TaskInstanceRepositoryAdapter(
    private val jpa: TaskInstanceJpaRepository,
    private val templateJpa: TaskTemplateJpaRepository,
    private val assignmentJpa: TaskAssignmentJpaRepository
) : TaskInstanceRepository {

    private val mapper = jacksonObjectMapper()

    override fun saveAll(instances: List<TaskInstance>): List<TaskInstance> =
        jpa.saveAll(instances.map { it.toEntity() }).map { it.toDomain() }

    override fun findByTaskTemplateId(taskTemplateId: String): List<TaskInstance> =
        jpa.findByTaskTemplateIdOrderByInstanceIndex(taskTemplateId).map { it.toDomain() }

    override fun deleteByTaskTemplateId(taskTemplateId: String) {
        assignmentJpa.clearTaskInstanceIdByTemplateId(taskTemplateId)
        jpa.deleteByTaskTemplateId(taskTemplateId)
    }

    private fun TaskInstance.toEntity(): TaskInstanceEntity {
        val templateRef = templateJpa.getReferenceById(taskTemplateId)
        return TaskInstanceEntity(
            id = id,
            taskTemplate = templateRef,
            instanceIndex = instanceIndex,
            options = mapper.writeValueAsString(options),
            correctAnswer = correctAnswer
        )
    }

    private fun TaskInstanceEntity.toDomain() = TaskInstance(
        id = id,
        taskTemplateId = taskTemplate.id,
        instanceIndex = instanceIndex,
        options = mapper.readValue(options),
        correctAnswer = correctAnswer
    )
}
