package com.ureka.play4change.features.auth.presentation

import com.ureka.play4change.core.component.base.ComponentEvents

sealed interface LoginEvents : ComponentEvents {
    data class EmailChanged(val email: String) : LoginEvents
    data object Submit : LoginEvents
    data object Resend : LoginEvents
    data object OpenAbout : LoginEvents
}
