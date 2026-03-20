package com.ureka.play4change.features.task.presentation

import com.ureka.play4change.core.component.base.ComponentEvents

sealed interface TaskEvents : ComponentEvents {
    // Legacy single-question events
    data class SelectOption(val index: Int) : TaskEvents
    data object ToggleHint : TaskEvents
    data object Submit : TaskEvents

    // Quiz events
    data class SelectAnswer(val questionIndex: Int, val optionIndex: Int) : TaskEvents
    data object SkipQuestion   : TaskEvents   // skip current question
    data object AutoAdvance    : TaskEvents   // fired by countdown timer after answer shown
    data object SubmitOrReview : TaskEvents   // submit if no skipped, else enter review mode
    data object SubmitFinal    : TaskEvents   // fired after all skipped questions answered

    // Step events
    data object NextStep : TaskEvents
    data class PhotoCaptured(val uri: String) : TaskEvents
    data object SubmitTask : TaskEvents

    // Shared
    data object Continue : TaskEvents
    // ExitRequested is REMOVED — back is blocked during an active task
}
