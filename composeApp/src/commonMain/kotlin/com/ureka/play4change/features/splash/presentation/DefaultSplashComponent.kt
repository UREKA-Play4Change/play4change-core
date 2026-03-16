package com.ureka.play4change.features.splash.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.core.component.stateful.safeLaunch
import com.ureka.play4change.features.splash.domain.repository.SplashRepository

class DefaultSplashComponent(
    componentContext: ComponentContext,
    private val repository: SplashRepository
) : BaseComponent<SplashState, SplashEvents>(componentContext, SplashState()), SplashComponent {

    init {
        checkSession()
    }

    private fun checkSession() {
        safeLaunch(scope) {
            val data = repository.checkSession()
            updateState { copy(isLoading = false) }
            if (data.isAuthenticated) {
                emitEffect(SplashEffect.NavigateToHome)
            } else {
                emitEffect(SplashEffect.NavigateToLogin)
            }
        }
    }

    override fun onEvent(event: SplashEvents) {
        // No events for splash
    }

    override fun SplashState.copyBase(isLoading: Boolean, error: AppError?): SplashState =
        copy(isLoading = isLoading, error = error)
}
