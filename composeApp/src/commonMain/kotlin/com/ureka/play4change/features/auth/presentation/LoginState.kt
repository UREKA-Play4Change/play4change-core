package com.ureka.play4change.features.auth.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.AppError

data class LoginState(
    override val isLoading: Boolean = false,
    override val error: AppError? = null,
    val email: String = "",
    val emailError: String? = null,
    val linkSent: Boolean = false,
    val resendCountdown: Int = 0
) : ComponentState
