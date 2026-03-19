package com.ureka.play4change.features.auth.presentation

import com.ureka.play4change.core.component.base.ComponentEvents
import com.ureka.play4change.features.auth.domain.model.SocialProvider

sealed interface LoginEvents : ComponentEvents {
    data class EmailChanged(val email: String) : LoginEvents
    data class NameChanged(val value: String) : LoginEvents
    data object Submit : LoginEvents
    data object Resend : LoginEvents
    data class SocialLogin(val provider: SocialProvider) : LoginEvents
    data object ToggleMode : LoginEvents
    data object OpenAbout : LoginEvents
}
