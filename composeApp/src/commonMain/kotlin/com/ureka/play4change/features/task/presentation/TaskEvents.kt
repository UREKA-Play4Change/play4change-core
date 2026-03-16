package com.ureka.play4change.features.task.presentation

import com.ureka.play4change.core.component.base.ComponentEvents

sealed interface TaskEvents : ComponentEvents {
    data class SelectOption(val index: Int) : TaskEvents
    data object ToggleHint : TaskEvents
    data object Submit : TaskEvents
    data object Continue : TaskEvents
    data object ExitRequested : TaskEvents
}
