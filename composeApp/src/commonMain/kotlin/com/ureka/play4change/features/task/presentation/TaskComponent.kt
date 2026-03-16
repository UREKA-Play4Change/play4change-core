package com.ureka.play4change.features.task.presentation

import com.arkivanov.decompose.value.Value
import com.ureka.play4change.core.component.base.BaseComponent
import kotlinx.coroutines.flow.Flow

interface TaskComponent {
    val state: Value<TaskState>
    val effects: Flow<BaseComponent.Effect>
    fun onEvent(event: TaskEvents)
}
