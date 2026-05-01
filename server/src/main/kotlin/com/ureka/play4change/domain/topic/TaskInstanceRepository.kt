package com.ureka.play4change.domain.topic

interface TaskInstanceRepository {
    fun saveAll(instances: List<TaskInstance>): List<TaskInstance>
    fun findByTaskTemplateId(taskTemplateId: String): List<TaskInstance>
}
