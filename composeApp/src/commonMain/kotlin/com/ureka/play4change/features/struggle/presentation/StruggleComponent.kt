package com.ureka.play4change.features.struggle.presentation

import com.arkivanov.decompose.value.Value
import com.ureka.play4change.core.component.base.BaseComponent
import kotlinx.coroutines.flow.Flow

interface StruggleComponent {
    val state: Value<StruggleState>
    val effects: Flow<BaseComponent.Effect>
    fun onEvent(event: StruggleEvents)
}
