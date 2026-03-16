package com.ureka.play4change.features.auth.presentation

import com.ureka.play4change.core.component.base.BaseComponent

sealed class LoginEffect : BaseComponent.Effect {
    data object NavigateToAbout : LoginEffect()
    data object NavigateToHome : LoginEffect()
}
