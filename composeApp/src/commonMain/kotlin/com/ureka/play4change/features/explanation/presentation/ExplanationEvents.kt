package com.ureka.play4change.features.explanation.presentation

import com.ureka.play4change.core.component.base.ComponentEvents

sealed interface ExplanationEvents : ComponentEvents {
    data object Understood : ExplanationEvents
    data object ToggleInput : ExplanationEvents
    data class InputChanged(val text: String) : ExplanationEvents
    data object SendMessage : ExplanationEvents
    data object RetryLoad : ExplanationEvents
}
