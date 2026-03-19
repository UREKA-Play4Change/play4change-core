package com.ureka.play4change.features.splash.presentation

import com.arkivanov.decompose.ComponentContext
import com.ureka.play4change.core.component.base.BaseComponent
import com.ureka.play4change.core.error.AppError
import com.ureka.play4change.core.component.stateful.safeLaunch
import com.ureka.play4change.features.splash.domain.repository.SplashRepository
import kotlinx.coroutines.delay
import kotlin.time.Clock

class DefaultSplashComponent(
    componentContext: ComponentContext,
    private val repository: SplashRepository
) : BaseComponent<SplashState, SplashEvents>(componentContext, SplashState()), SplashComponent {

    init {
        checkSession()
    }

    private fun checkSession() {
        safeLaunch(scope) {
            val start = Clock.System.now().toEpochMilliseconds()
            val data = repository.checkSession()
            val elapsed = Clock.System.now().toEpochMilliseconds() - start
            val remaining = 3500L - elapsed
            if (remaining > 0) delay(remaining)
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
