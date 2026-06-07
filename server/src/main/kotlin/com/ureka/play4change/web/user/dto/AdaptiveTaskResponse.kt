package com.ureka.play4change.web.user.dto

import com.ureka.play4change.domain.struggle.AdaptiveTask

data class AdaptiveTaskResponse(
    val taskId: String,
    val title: String,
    val description: String,
    val hint: String?,
    val options: List<String>,
    val pointsReward: Int,
    // True when the learner already submitted this task in a previous session of the app.
    // The client uses this to restore currentIndex after a cold restart so the user
    // continues where they left off instead of being sent back to task 0.
    val isCompleted: Boolean
) {
    companion object {
        fun from(task: AdaptiveTask): AdaptiveTaskResponse {
            val rawOptions = task.options ?: emptyList()
            val shuffledOptions = if (task.optionOrder.isNotEmpty())
                task.optionOrder.mapNotNull { rawOptions.getOrNull(it) }
            else
                rawOptions
            return AdaptiveTaskResponse(
                taskId = task.id,
                title = task.title,
                description = task.description,
                hint = task.hint,
                options = shuffledOptions,
                pointsReward = task.pointsReward,
                isCompleted = task.completedAt != null
            )
        }
    }
}
