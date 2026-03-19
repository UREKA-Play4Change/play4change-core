package com.ureka.play4change.features.auth.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.AppError

enum class AuthMode { Login, Register }
enum class LoginStage { EmailEntry, LinkSent }

data class LoginState(
    val mode: AuthMode = AuthMode.Login,
    val name: String = "",
    val nameError: String? = null,
    val email: String = "",
    val emailError: String? = null,
    val stage: LoginStage = LoginStage.EmailEntry,
    val resendCountdown: Int = 0,
    // Legacy field for backward compat
    val linkSent: Boolean = false,
    override val isLoading: Boolean = false,
    override val error: AppError? = null
) : ComponentState
