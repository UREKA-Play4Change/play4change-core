package com.ureka.play4change.features.auth.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.core.component.stateful.safeLaunch
import com.ureka.play4change.features.auth.domain.model.SocialProvider
import com.ureka.play4change.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DefaultLoginComponent(
    componentContext: ComponentContext,
    private val repository: AuthRepository,
    private val onNavigateToAbout: () -> Unit
) : BaseComponent<LoginState, LoginEvents>(componentContext, LoginState()), LoginComponent {

    private var countdownJob: Job? = null

    override fun onEvent(event: LoginEvents) {
        when (event) {
            is LoginEvents.EmailChanged -> updateState { copy(email = event.email, emailError = null) }
            is LoginEvents.NameChanged  -> updateState { copy(name = event.value, nameError = null) }
            LoginEvents.Submit          -> emitEffect(LoginEffect.NavigateToHome)
            LoginEvents.Resend          -> resend()
            is LoginEvents.SocialLogin  -> handleSocialLogin(event.provider)
            LoginEvents.ToggleMode      -> updateState {
                copy(
                    mode = if (mode == AuthMode.Login) AuthMode.Register else AuthMode.Login,
                    stage = LoginStage.EmailEntry,
                    email = "", name = "", emailError = null, nameError = null
                )
            }
            LoginEvents.OpenAbout       -> emitEffect(LoginEffect.NavigateToAbout)
        }
    }

    private fun handleSocialLogin(provider: SocialProvider) {
        safeLaunch(scope) {
            updateState { copy(isLoading = true) }
            repository.socialLogin(provider)?.let {
                emitEffect(LoginEffect.NavigateToHome)
            } ?: updateState {
                copy(isLoading = false, error = AppError.ServerError.Unexpected("Social login failed"))
            }
        }
    }

    private fun resend() {
        val email = state.value.email.trim()
        safeLaunch(scope) {
            repository.sendMagicLink(email)
            startCountdown()
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (i in 60 downTo 0) {
                updateState { copy(resendCountdown = i) }
                if (i == 0) break
                delay(1000)
            }
        }
    }

    override fun LoginState.copyBase(isLoading: Boolean, error: AppError?): LoginState =
        copy(isLoading = isLoading, error = error)
}
