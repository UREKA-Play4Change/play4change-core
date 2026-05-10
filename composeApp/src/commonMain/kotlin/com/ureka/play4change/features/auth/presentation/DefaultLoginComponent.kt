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
) : BaseComponent<LoginState, LoginEvents>(componentContext, LoginState()), LoginComponent {

    private var countdownJob: Job? = null

    override fun onEvent(event: LoginEvents) {
        when (event) {
            is LoginEvents.EmailChanged  -> updateState { copy(email = event.email, emailError = null) }
            LoginEvents.Submit           -> submit()
            LoginEvents.Resend           -> resend()
            is LoginEvents.SocialLogin   -> handleSocialLogin(event.provider)
            is LoginEvents.TokenChanged  -> updateState { copy(tokenInput = event.value, error = null) }
            LoginEvents.VerifyToken      -> verifyToken()
        }
    }

    private fun submit() {
        val email = state.value.email.trim()
        if (email.isBlank() || !email.contains('@')) {
            updateState { copy(emailError = "Invalid email") }
            return
        }
        scope.launch {
            updateState { copy(loadingAction = LoginLoadingAction.Email, error = null) }
            try {
                repository.sendMagicLink(email)
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

    private fun verifyToken() {
        val token = state.value.tokenInput.trim()
        if (token.isBlank()) return
        scope.launch {
            updateState { copy(loadingAction = LoginLoadingAction.Token, error = null) }
            try {
                val result = repository.verifyMagicLink(token)
                if (result != null) {
                    emitEffect(LoginEffect.NavigateToHome)
                } else {
                    updateState {
                        copy(
                            loadingAction = null,
                            error = AppError.ServerError.Unexpected("Token verification failed")
                        )
                    }
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
