package com.ureka.play4change.features.auth.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.features.auth.domain.model.SocialProvider
import com.ureka.play4change.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

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
            LoginEvents.Submit          -> submit()
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

    private fun submit() {
        val current = state.value
        val email = current.email.trim()
        if (email.isBlank() || !email.contains('@')) {
            updateState { copy(emailError = "Invalid email") }
            return
        }
        scope.launch {
            updateState { copy(loadingAction = LoginLoadingAction.Email, error = null) }
            try {
                if (current.mode == AuthMode.Register) {
                    repository.register(current.name.trim(), email)
                } else {
                    repository.sendMagicLink(email)
                }
                updateState { copy(loadingAction = null, linkSent = true, stage = LoginStage.LinkSent) }
                startCountdown()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateState {
                    copy(
                        loadingAction = null,
                        error = AppError.ServerError.Unexpected(e.message)
                    )
                }
            }
        }
    }

    private fun handleSocialLogin(provider: SocialProvider) {
        scope.launch {
            updateState { copy(loadingAction = LoginLoadingAction.Social(provider), error = null) }
            try {
                repository.socialLogin(provider)?.let {
                    emitEffect(LoginEffect.NavigateToHome)
                } ?: updateState {
                    copy(
                        loadingAction = null,
                        error = AppError.ServerError.Unexpected("Social login failed")
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateState {
                    copy(
                        loadingAction = null,
                        error = AppError.ServerError.Unexpected(e.message)
                    )
                }
            }
        }
    }

    private fun resend() {
        val email = state.value.email.trim()
        scope.launch {
            updateState { copy(loadingAction = LoginLoadingAction.Email, error = null) }
            try {
                repository.sendMagicLink(email)
                updateState { copy(loadingAction = null) }
                startCountdown()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateState {
                    copy(
                        loadingAction = null,
                        error = AppError.ServerError.Unexpected(e.message)
                    )
                }
            }
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
