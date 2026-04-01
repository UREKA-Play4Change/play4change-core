package com.ureka.play4change.domain.topic

interface TaskTemplateRepository {
    fun saveAll(templates: List<TaskTemplate>): List<TaskTemplate>
    fun findCurrentByModuleId(moduleId: String): List<TaskTemplate>
    fun markAllSuperseded(moduleId: String)
}
