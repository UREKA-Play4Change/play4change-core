package com.ureka.play4change.infrastructure.persistence.adapter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ureka.play4change.domain.topic.TaskTemplate
import com.ureka.play4change.domain.topic.TaskTemplateRepository
import com.ureka.play4change.domain.topic.TaskType
import com.ureka.play4change.infrastructure.persistence.entity.TaskTemplateEntity
import com.ureka.play4change.infrastructure.persistence.repository.TaskTemplateJpaRepository
import com.ureka.play4change.infrastructure.persistence.repository.TopicModuleJpaRepository
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class TaskTemplateRepositoryAdapter(
    private val jpa: TaskTemplateJpaRepository,
    private val moduleJpa: TopicModuleJpaRepository
) : TaskTemplateRepository {

    private val mapper = jacksonObjectMapper()

    override fun saveAll(templates: List<TaskTemplate>): List<TaskTemplate> =
        jpa.saveAll(templates.map { it.toEntity() }).map { it.toDomain() }

    override fun findCurrentByModuleId(moduleId: String): List<TaskTemplate> =
        jpa.findCurrentByModuleId(moduleId).map { it.toDomain() }

    override fun markAllSuperseded(moduleId: String) {
        val current = jpa.findCurrentByModuleId(moduleId)
        current.forEach { it.isCurrent = false }
        jpa.saveAll(current)
    }

    private fun TaskTemplateEntity.toDomain() = TaskTemplate(
        id = id,
        moduleId = module.id,
        dayIndex = dayIndex,
        poolIndex = poolIndex,
        title = title,
        description = description,
        hint = hint,
        taskType = TaskType.valueOf(taskType),
        pointsReward = pointsReward,
        options = options?.let { runCatching { mapper.readValue<List<String>>(it) }.getOrNull() },
        correctAnswer = correctAnswer,
        version = version,
        isCurrent = isCurrent,
        supersededBy = supersededBy,
        embedding = embedding,
        createdAt = createdAt
    )

    private fun TaskTemplate.toEntity(): TaskTemplateEntity {
        val moduleRef = moduleJpa.getReferenceById(moduleId)
        return TaskTemplateEntity(
            id = id,
            module = moduleRef,
            dayIndex = dayIndex,
            poolIndex = poolIndex,
            title = title,
            description = description,
            hint = hint,
            taskType = taskType.name,
            pointsReward = pointsReward,
            options = options?.let { runCatching { mapper.writeValueAsString(it) }.getOrNull() },
            correctAnswer = correctAnswer,
            version = version,
            isCurrent = isCurrent,
            supersededBy = supersededBy,
            embedding = embedding,
            createdAt = createdAt
        )
    }
}
