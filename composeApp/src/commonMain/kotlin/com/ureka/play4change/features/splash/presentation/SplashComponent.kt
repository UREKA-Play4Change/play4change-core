package com.ureka.play4change.features.splash.presentation

import com.arkivanov.decompose.value.Value
import com.ureka.play4change.core.component.base.BaseComponent
import kotlinx.coroutines.flow.Flow

interface SplashComponent {
    val state: Value<SplashState>
    val effects: Flow<BaseComponent.Effect>
    fun onEvent(event: SplashEvents)
}
