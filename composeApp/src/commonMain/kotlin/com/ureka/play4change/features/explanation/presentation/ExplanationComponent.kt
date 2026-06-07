package com.ureka.play4change.features.explanation.presentation

import com.arkivanov.decompose.value.Value
import com.ureka.play4change.core.component.base.BaseComponent
import kotlinx.coroutines.flow.Flow

interface ExplanationComponent {
    val state: Value<ExplanationState>
    val effects: Flow<BaseComponent.Effect>
    fun onEvent(event: ExplanationEvents)
}
