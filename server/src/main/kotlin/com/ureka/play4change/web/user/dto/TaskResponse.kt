package com.ureka.play4change.web.user.dto

import com.ureka.play4change.domain.enrollment.TaskAssignment
import com.ureka.play4change.domain.topic.TaskTemplate
import java.time.OffsetDateTime

data class TaskResponse(
    val assignmentId: String,
    val title: String,
    val description: String,
    val hint: String?,
    val options: List<String>,
    val pointsReward: Int,
    val dueAt: OffsetDateTime,
    val wrongAttemptCount: Int
) {
    companion object {
        fun from(assignment: TaskAssignment, template: TaskTemplate): TaskResponse {
            val shuffledOptions = assignment.optionOrder
                .mapNotNull { originalIdx -> template.options?.getOrNull(originalIdx) }
            return TaskResponse(
                assignmentId = assignment.id,
                title = template.title,
                description = template.description,
                hint = template.hint,
                options = shuffledOptions,
                pointsReward = template.pointsReward,
                dueAt = assignment.dueAt,
                wrongAttemptCount = assignment.wrongAttemptCount
            )
        }
    }
}
