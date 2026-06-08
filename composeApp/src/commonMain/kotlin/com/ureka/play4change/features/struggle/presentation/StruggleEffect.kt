package com.ureka.play4change.features.struggle.presentation

import com.ureka.play4change.core.component.base.BaseComponent

sealed class StruggleEffect : BaseComponent.Effect {
    /** Struggle resolved with success — return to home so the learner retries the original task. */
    data object NavigateToHome : StruggleEffect()
    /** Max depth reached — go directly to the explanation screen without a home detour. */
    data class NavigateToExplanation(val sessionId: String) : StruggleEffect()
}
