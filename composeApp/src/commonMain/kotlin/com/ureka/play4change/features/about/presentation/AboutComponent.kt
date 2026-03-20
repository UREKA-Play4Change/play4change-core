package com.ureka.play4change.features.about.presentation

import com.arkivanov.decompose.value.Value
import com.ureka.play4change.core.component.base.BaseComponent
import kotlinx.coroutines.flow.Flow

interface AboutComponent {
    val state: Value<AboutState>
    val effects: Flow<BaseComponent.Effect>
    fun onEvent(event: AboutEvents)
}
