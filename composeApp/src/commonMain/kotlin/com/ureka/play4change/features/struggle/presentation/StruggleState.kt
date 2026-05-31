package com.ureka.play4change.features.struggle.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.features.struggle.domain.model.AdaptiveTask

data class StruggleState(
    override val isLoading: Boolean = true,
    override val error: AppError? = null,
    val tasks: List<AdaptiveTask> = emptyList(),
    val sessionId: String = "",
    val errorPattern: String = "",
    val currentIndex: Int = 0,
    val selectedIndex: Int? = null,
    val submitted: Boolean = false,
    val isCorrect: Boolean = false,
    val wrongAnswerFeedback: Boolean = false,
    val pointsAwarded: Int = 0,
    val sessionResolved: Boolean = false,
) : ComponentState {
    val currentTask: AdaptiveTask? get() = tasks.getOrNull(currentIndex)
    val isLastTask: Boolean get() = currentIndex == tasks.lastIndex
}
