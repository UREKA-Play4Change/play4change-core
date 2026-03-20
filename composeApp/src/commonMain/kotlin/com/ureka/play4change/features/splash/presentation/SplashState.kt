package com.ureka.play4change.features.splash.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.AppError

data class SplashState(
    override val isLoading: Boolean = true,
    override val error: AppError? = null
) : ComponentState
