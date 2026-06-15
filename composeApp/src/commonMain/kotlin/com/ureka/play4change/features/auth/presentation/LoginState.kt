package com.ureka.play4change.features.auth.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.UiError

enum class LoginStage { EmailEntry, LinkSent }

sealed class LoginLoadingAction {
    data object Email : LoginLoadingAction()
    data object Token : LoginLoadingAction()
}

data class LoginState(
    val email: String = "",
    val emailError: String? = null,
    val stage: LoginStage = LoginStage.EmailEntry,
    val resendCountdown: Int = 0,
    val linkSent: Boolean = false,
    val loadingAction: LoginLoadingAction? = null,
    val tokenInput: String = "",
    override val isLoading: Boolean = false,
    override val error: UiError? = null
) : ComponentState

val LoginState.isEmailLoading: Boolean get() = loadingAction is LoginLoadingAction.Email
