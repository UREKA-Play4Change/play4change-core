package com.ureka.play4change.features.task.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.features.task.domain.model.TaskDetail

data class TaskState(
    override val isLoading: Boolean = true,
    override val error: AppError? = null,
    val task: TaskDetail? = null,
    val selectedIndex: Int? = null,
    val hintVisible: Boolean = false,
    val submitted: Boolean = false,
    val isCorrect: Boolean = false,
    val pointsAwarded: Int = 0
) : ComponentState
