package com.ureka.play4change.features.auth.presentation

import com.ureka.play4change.core.component.base.ComponentEvents

sealed interface LoginEvents : ComponentEvents {
    data class EmailChanged(val email: String) : LoginEvents
    data object Submit : LoginEvents
    data object Resend : LoginEvents
    /** Debug builds only: operator pastes raw token from server logs. */
    data class TokenChanged(val value: String) : LoginEvents
    /** Debug builds only: trigger POST /auth/magic-link/verify with the pasted token. */
    data object VerifyToken : LoginEvents
}
