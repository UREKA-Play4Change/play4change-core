package com.ureka.play4change.features.explanation.presentation

import com.ureka.play4change.core.component.base.BaseComponent

sealed class ExplanationEffect : BaseComponent.Effect {
    data object NavigateToHome : ExplanationEffect()
}
