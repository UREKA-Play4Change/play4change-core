package com.ureka.play4change.features.profile.presentation

import com.arkivanov.decompose.value.Value
import com.ureka.play4change.core.component.base.BaseComponent
import kotlinx.coroutines.flow.Flow

interface ProfileComponent {
    val state: Value<ProfileState>
    val effects: Flow<BaseComponent.Effect>
    fun onEvent(event: ProfileEvents)
}
