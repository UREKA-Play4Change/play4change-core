package com.ureka.play4change.features.profile.presentation

import com.ureka.play4change.core.component.base.BaseComponent

sealed class ProfileEffect : BaseComponent.Effect {
    data object NavigateToAbout : ProfileEffect()
    data object SignedOut : ProfileEffect()
}
