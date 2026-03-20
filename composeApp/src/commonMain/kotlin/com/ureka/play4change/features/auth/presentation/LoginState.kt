package com.ureka.play4change.features.auth.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.features.auth.domain.model.SocialProvider

enum class AuthMode { Login, Register }
enum class LoginStage { EmailEntry, LinkSent }

sealed class LoginLoadingAction {
    data object Email : LoginLoadingAction()
    data class Social(val provider: SocialProvider) : LoginLoadingAction()
}

data class LoginState(
    val mode: AuthMode = AuthMode.Login,
    val name: String = "",
    val nameError: String? = null,
    val email: String = "",
    val emailError: String? = null,
    val stage: LoginStage = LoginStage.EmailEntry,
    val resendCountdown: Int = 0,
    val linkSent: Boolean = false,
    val loadingAction: LoginLoadingAction? = null,
    override val isLoading: Boolean = false,
    override val error: AppError? = null
) : ComponentState

val LoginState.isEmailLoading: Boolean get() = loadingAction is LoginLoadingAction.Email
val LoginState.loadingProvider: SocialProvider? get() = (loadingAction as? LoginLoadingAction.Social)?.provider
