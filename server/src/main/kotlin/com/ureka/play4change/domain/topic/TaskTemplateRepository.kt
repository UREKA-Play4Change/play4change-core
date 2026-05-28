package com.ureka.play4change.domain.topic

interface TaskTemplateRepository {
    fun saveAll(templates: List<TaskTemplate>): List<TaskTemplate>
    fun save(template: TaskTemplate): TaskTemplate
    fun findById(id: String): TaskTemplate?
    fun findCurrentByModuleId(moduleId: String): List<TaskTemplate>
    fun findCurrentByModuleIdAndDayIndexAndLanguage(moduleId: String, dayIndex: Int, language: String): TaskTemplate?
    fun markAllSuperseded(moduleId: String)
    fun findCurrentByTopicId(topicId: String): List<TaskTemplate>
}
