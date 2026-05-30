package com.ureka.play4change.features.struggle.presentation

import com.ureka.play4change.core.component.base.BaseComponent

sealed class StruggleEffect : BaseComponent.Effect {
    /** Struggle resolved — return to home so the learner retries the original task. */
    data object NavigateToHome : StruggleEffect()
}
