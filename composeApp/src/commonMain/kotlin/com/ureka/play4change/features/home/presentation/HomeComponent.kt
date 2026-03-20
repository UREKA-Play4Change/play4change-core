package com.ureka.play4change.features.home.presentation

import com.arkivanov.decompose.value.Value
import com.ureka.play4change.core.component.base.BaseComponent
import kotlinx.coroutines.flow.Flow

interface HomeComponent {
    val state: Value<HomeState>
    val effects: Flow<BaseComponent.Effect>
    fun onEvent(event: HomeEvents)
}
