package com.ureka.play4change.web.user.dto

import com.ureka.play4change.domain.struggle.AdaptiveTask

data class AdaptiveTaskResponse(
    val taskId: String,
    val title: String,
    val description: String,
    val hint: String?,
    val options: List<String>,
    val pointsReward: Int
) {
    companion object {
        fun from(task: AdaptiveTask): AdaptiveTaskResponse {
            val shuffledOptions = task.optionOrder
                .mapNotNull { originalIdx -> task.options?.getOrNull(originalIdx) }
            return AdaptiveTaskResponse(
                taskId = task.id,
                title = task.title,
                description = task.description,
                hint = task.hint,
                options = shuffledOptions,
                pointsReward = task.pointsReward
            )
        }
    }
}
