package com.ureka.play4change.features.about.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.AppError

data class AboutState(
    override val isLoading: Boolean = false,
    override val error: AppError? = null
) : ComponentState
