package com.ureka.play4change.features.profile.presentation

import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.features.about.presentation.AboutEffect

sealed class ProfileEffect : BaseComponent.Effect {
    data object NavigateBack : ProfileEffect()
    data object NavigateToAbout : ProfileEffect()
    data object SignedOut : ProfileEffect()
}
