package com.ureka.play4change.features.about.presentation

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.error.UiError

data class AboutState(
    override val isLoading: Boolean = false,
    override val error: UiError? = null
) : ComponentState
