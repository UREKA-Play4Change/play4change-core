package com.ureka.play4change.web.admin.dto

import com.ureka.play4change.application.port.TaskTemplateWithStats
import com.ureka.play4change.domain.topic.TaskQuestionStats
import com.ureka.play4change.domain.topic.TaskTemplate
import java.time.OffsetDateTime

data class TaskQuestionStatsResponse(
    val totalAttempts: Int,
    val successCount: Int,
    val successRate: Double,
    val avgPointsAwarded: Double
)

data class TaskTemplateAdminResponse(
    val id: String,
    val dayIndex: Int,
    val poolIndex: Int,
    val title: String,
    val description: String,
    val hint: String?,
    val taskType: String,
    val pointsReward: Int,
    val options: List<String>?,
    val correctAnswer: Int?,
    val version: Int,
    val language: String,
    val createdAt: OffsetDateTime,
    val stats: TaskQuestionStatsResponse
) {
    companion object {
        fun from(item: TaskTemplateWithStats): TaskTemplateAdminResponse =
            from(item.template, item.stats)

        private fun from(template: TaskTemplate, stats: TaskQuestionStats) = TaskTemplateAdminResponse(
            id = template.id,
            dayIndex = template.dayIndex,
            poolIndex = template.poolIndex,
            title = template.title,
            description = template.description,
            hint = template.hint,
            taskType = template.taskType.name,
            pointsReward = template.pointsReward,
            options = template.options,
            correctAnswer = template.correctAnswer,
            version = template.version,
            language = template.language,
            createdAt = template.createdAt,
            stats = TaskQuestionStatsResponse(
                totalAttempts = stats.totalAttempts,
                successCount = stats.successCount,
                successRate = stats.successRate,
                avgPointsAwarded = stats.avgPointsAwarded
            )
        )
    }
}
