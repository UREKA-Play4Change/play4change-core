package com.ureka.play4change.features.task.presentation

import com.ureka.play4change.core.component.base.ComponentEvents

sealed interface TaskEvents : ComponentEvents {
    // Legacy single-question events
    data class SelectOption(val index: Int) : TaskEvents
    data object ToggleHint : TaskEvents
    data object Submit : TaskEvents
    // Quiz events
    data class SelectAnswer(val questionIndex: Int, val optionIndex: Int) : TaskEvents
    data object NextQuestion : TaskEvents
    data object SubmitQuiz : TaskEvents
    // Step events
    data object NextStep : TaskEvents
    data class PhotoCaptured(val uri: String) : TaskEvents
    data object SubmitTask : TaskEvents
    // Shared
    data object Continue : TaskEvents
    data object ExitRequested : TaskEvents
}
