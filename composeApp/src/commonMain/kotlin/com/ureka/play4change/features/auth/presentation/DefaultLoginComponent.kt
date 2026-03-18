package com.ureka.play4change.features.auth.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.core.component.stateful.safeLaunch
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
            LoginEvents.Submit          -> emitEffect(LoginEffect.NavigateToHome)//submit()
            LoginEvents.Resend          -> resend()
            LoginEvents.OpenAbout       -> emitEffect(LoginEffect.NavigateToAbout)
        }
    }

    private fun submit() {
        val email = state.value.email.trim()
        if (!email.contains('@') || !email.contains('.')) {
            updateState { copy(emailError = "login_email_error_invalid") }
            return
        }
        safeLaunch(scope) {
            repository.sendMagicLink(email)
            updateState { copy(linkSent = true) }
            startCountdown()
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
