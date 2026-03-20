package com.ureka.play4change.features.task.presentation

import com.ureka.play4change.core.component.base.BaseComponent

sealed class TaskEffect : BaseComponent.Effect {
    data object NavigateBack : TaskEffect()
}
