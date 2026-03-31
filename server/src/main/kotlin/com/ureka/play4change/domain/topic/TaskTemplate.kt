package com.ureka.play4change.domain.topic

import java.time.OffsetDateTime
import java.util.UUID

data class TaskTemplate(
    val id: String,
    val moduleId: String,
    val dayIndex: Int,
    val poolIndex: Int,
    val title: String,
    val description: String,
    val hint: String?,
    val taskType: TaskType,
    val pointsReward: Int,
    val options: List<String>?,
    val correctAnswer: Int?,
    val version: Int,
    val isCurrent: Boolean,
    val supersededBy: String?,
    val embedding: FloatArray?,
    val createdAt: OffsetDateTime
) {
    fun nextVersion(newId: String = UUID.randomUUID().toString()): TaskTemplate =
        copy(
            id = newId,
            version = version + 1,
            isCurrent = true,
            supersededBy = null,
            embedding = null,
            createdAt = OffsetDateTime.now()
        )
}
