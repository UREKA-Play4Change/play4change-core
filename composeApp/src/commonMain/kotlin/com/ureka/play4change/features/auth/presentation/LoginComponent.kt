package com.ureka.play4change.features.auth.presentation

import com.arkivanov.decompose.value.Value
import com.ureka.play4change.core.component.base.BaseComponent
import kotlinx.coroutines.flow.Flow

interface LoginComponent {
    val state: Value<LoginState>
    val effects: Flow<BaseComponent.Effect>
    fun onEvent(event: LoginEvents)
}
