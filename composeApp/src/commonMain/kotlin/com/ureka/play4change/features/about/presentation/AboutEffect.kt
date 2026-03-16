package com.ureka.play4change.features.about.presentation

import com.ureka.play4change.core.component.base.BaseComponent

sealed class AboutEffect : BaseComponent.Effect {
    data object NavigateBack : AboutEffect()
}
