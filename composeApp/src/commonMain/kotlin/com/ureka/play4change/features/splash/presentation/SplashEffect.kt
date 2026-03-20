package com.ureka.play4change.features.splash.presentation

import com.ureka.play4change.core.component.base.BaseComponent

sealed class SplashEffect : BaseComponent.Effect {
    data object NavigateToLogin : SplashEffect()
    data object NavigateToHome : SplashEffect()
}
